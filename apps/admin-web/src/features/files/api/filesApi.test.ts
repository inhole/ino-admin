import { afterEach, beforeEach, expect, test, vi } from "vitest";
import { uploadFile } from "@/features/files/api/filesApi";

interface XhrResult {
  status: number;
  response: unknown;
  loaded?: number;
  total?: number;
}

class FakeEventTarget {
  private readonly listeners = new Map<string, EventListener[]>();

  addEventListener(type: string, listener: EventListenerOrEventListenerObject) {
    if (typeof listener !== "function") return;
    const current = this.listeners.get(type) ?? [];
    current.push(listener);
    this.listeners.set(type, current);
  }

  dispatch(type: string, event: Event) {
    this.listeners.get(type)?.forEach((listener) => listener(event));
  }
}

class FakeXMLHttpRequest extends FakeEventTarget {
  static results: XhrResult[] = [];
  static instances: FakeXMLHttpRequest[] = [];

  readonly upload = new FakeEventTarget();
  readonly headers = new Headers();
  method = "";
  url = "";
  body: Document | XMLHttpRequestBodyInit | null = null;
  status = 0;
  response: unknown = null;
  responseType: XMLHttpRequestResponseType = "";

  constructor() {
    super();
    FakeXMLHttpRequest.instances.push(this);
  }

  open(method: string, url: string) {
    this.method = method;
    this.url = url;
  }

  setRequestHeader(name: string, value: string) {
    this.headers.set(name, value);
  }

  send(body: Document | XMLHttpRequestBodyInit | null) {
    this.body = body;
    const result = FakeXMLHttpRequest.results.shift();
    if (!result) throw new Error("Missing fake XMLHttpRequest result");
    queueMicrotask(() => {
      if (result.total !== undefined) {
        this.upload.dispatch(
          "progress",
          {
            lengthComputable: true,
            loaded: result.loaded ?? result.total,
            total: result.total,
          } as ProgressEvent,
        );
      }
      this.status = result.status;
      this.response = result.response;
      this.dispatch("load", new Event("load"));
    });
  }
}

const originalXMLHttpRequest = globalThis.XMLHttpRequest;

beforeEach(() => {
  sessionStorage.clear();
  FakeXMLHttpRequest.results = [];
  FakeXMLHttpRequest.instances = [];
  globalThis.XMLHttpRequest =
    FakeXMLHttpRequest as unknown as typeof XMLHttpRequest;
});

afterEach(() => {
  globalThis.XMLHttpRequest = originalXMLHttpRequest;
  vi.restoreAllMocks();
});

test("uploads multipart data with authorization and reports progress", async () => {
  sessionStorage.setItem("ino-admin.access-token", "access-token");
  FakeXMLHttpRequest.results.push({
    status: 201,
    response: {
      id: "file-1",
      originalName: "notes.txt",
      contentType: "text/plain",
      size: 5,
      createdAt: "2026-08-16T00:00:00Z",
    },
    loaded: 2,
    total: 4,
  });
  const progress: number[] = [];
  const file = new File(["notes"], "notes.txt", { type: "text/plain" });

  const uploaded = await uploadFile(file, (value) => progress.push(value));

  const request = FakeXMLHttpRequest.instances[0];
  expect(request.method).toBe("POST");
  expect(request.url).toBe("/api/v1/files");
  expect(request.headers.get("Authorization")).toBe("Bearer access-token");
  expect((request.body as FormData).get("file")).toBe(file);
  expect(progress).toEqual([50, 100]);
  expect(uploaded.originalName).toBe("notes.txt");
});

test("refreshes an expired session once and retries the upload", async () => {
  sessionStorage.setItem("ino-admin.access-token", "access-old");
  sessionStorage.setItem("ino-admin.refresh-token", "refresh-old");
  FakeXMLHttpRequest.results.push(
    {
      status: 401,
      response: { code: "UNAUTHORIZED", message: "인증이 필요합니다." },
    },
    {
      status: 201,
      response: {
        id: "file-1",
        originalName: "notes.txt",
        contentType: "text/plain",
        size: 5,
        createdAt: "2026-08-16T00:00:00Z",
      },
    },
  );
  const refresh = vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(
      JSON.stringify({
        accessToken: "access-new",
        tokenType: "Bearer",
        expiresIn: 900,
        refreshToken: "refresh-new",
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    ),
  );

  await uploadFile(
    new File(["notes"], "notes.txt", { type: "text/plain" }),
  );

  expect(refresh).toHaveBeenCalledTimes(1);
  expect(FakeXMLHttpRequest.instances).toHaveLength(2);
  expect(
    FakeXMLHttpRequest.instances[0].headers.get("Authorization"),
  ).toBe("Bearer access-old");
  expect(
    FakeXMLHttpRequest.instances[1].headers.get("Authorization"),
  ).toBe("Bearer access-new");
  expect(sessionStorage.getItem("ino-admin.refresh-token")).toBe(
    "refresh-new",
  );
});
