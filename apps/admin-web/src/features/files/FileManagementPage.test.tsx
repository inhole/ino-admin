import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, beforeEach, expect, test, vi } from "vitest";
import { ApiClientError } from "@/api/client";
import { FileManagementPage } from "@/features/files/FileManagementPage";

const fileApi = vi.hoisted(() => ({
  deleteFile: vi.fn(),
  downloadFile: vi.fn(),
  getMyFiles: vi.fn(),
  uploadFile: vi.fn(),
}));

vi.mock("@/features/files/api/filesApi", () => fileApi);

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={queryClient}>
      <FileManagementPage />
    </QueryClientProvider>,
  );
}

const storedFile = {
  id: "file-1",
  originalName: "report.pdf",
  contentType: "application/pdf",
  size: 2048,
  createdAt: "2026-08-16T00:00:00Z",
};

async function chooseFileAction(name: string) {
  fireEvent.click(
    screen.getByRole("button", { name: "report.pdf 작업 메뉴" }),
  );
  const item = await screen.findByRole("menuitem", { name });
  fireEvent.pointerDown(item, { pointerType: "mouse" });
  fireEvent.click(item);
}

beforeEach(() => {
  Object.values(fileApi).forEach((mock) => mock.mockReset());
  fileApi.getMyFiles.mockResolvedValue({
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  });
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

test("queues multiple dropped files and retries only the failed upload", async () => {
  const successful = new File(["ok"], "guide.pdf", {
    type: "application/pdf",
  });
  const retryable = new File(["retry"], "notes.txt", {
    type: "text/plain",
  });
  let retryableAttempts = 0;
  fileApi.uploadFile.mockImplementation(
    async (file: File, onProgress?: (percentage: number) => void) => {
      onProgress?.(50);
      if (file.name === "notes.txt" && retryableAttempts++ === 0) {
        throw new ApiClientError("서버가 파일을 거부했습니다.", 400);
      }
      return {
        id: file.name,
        originalName: file.name,
        contentType: file.type,
        size: file.size,
        createdAt: "2026-08-16T00:00:00Z",
      };
    },
  );

  renderPage();
  const dropZone = await screen.findByText("파일을 선택하거나 여기로 끌어오세요");
  fireEvent.drop(dropZone, {
    dataTransfer: { files: [successful, retryable] },
  });

  expect(screen.getByText("guide.pdf")).toBeInTheDocument();
  expect(screen.getByText("notes.txt")).toBeInTheDocument();
  expect(screen.getAllByText("대기")).toHaveLength(2);

  fireEvent.click(screen.getByRole("button", { name: "모두 업로드" }));

  expect(
    await screen.findByText("서버가 파일을 거부했습니다."),
  ).toBeInTheDocument();
  expect(screen.getByText("성공")).toBeInTheDocument();
  expect(screen.getByText("실패")).toBeInTheDocument();
  expect(fileApi.uploadFile).toHaveBeenCalledTimes(2);

  fireEvent.click(
    screen.getByRole("button", { name: "notes.txt 다시 업로드" }),
  );

  await waitFor(() => {
    expect(screen.getAllByText("성공")).toHaveLength(2);
  });
  expect(fileApi.uploadFile).toHaveBeenCalledTimes(3);
  expect(
    screen.queryByText("서버가 파일을 거부했습니다."),
  ).not.toBeInTheDocument();
});

test("adds multiple files through the labelled file input", async () => {
  renderPage();
  const input = await screen.findByLabelText(
    "파일을 선택하거나 여기로 끌어오세요",
  );
  const files = [
    new File(["one"], "one.txt", { type: "text/plain" }),
    new File(["two"], "two.txt", { type: "text/plain" }),
  ];

  fireEvent.change(input, { target: { files } });

  expect(screen.getByText("one.txt")).toBeInTheDocument();
  expect(screen.getByText("two.txt")).toBeInTheDocument();
  expect(screen.getByText("업로드 목록 2개")).toBeInTheDocument();
});

test("applies name, type, date, and sort filters to the file query", async () => {
  renderPage();
  await waitFor(() => expect(fileApi.getMyFiles).toHaveBeenCalled());

  fireEvent.change(screen.getByLabelText("파일 이름"), {
    target: { value: " report " },
  });
  fireEvent.change(screen.getByLabelText("업로드 시작일"), {
    target: { value: "2026-08-01" },
  });
  fireEvent.change(screen.getByLabelText("업로드 종료일"), {
    target: { value: "2026-08-31" },
  });

  fireEvent.click(screen.getByRole("combobox", { name: "파일 유형" }));
  const pdfOption = await screen.findByRole("option", { name: "PDF" });
  fireEvent.pointerDown(pdfOption, { pointerType: "mouse" });
  fireEvent.click(pdfOption);
  fireEvent.click(screen.getByRole("combobox", { name: "정렬" }));
  const nameOption = await screen.findByRole("option", {
    name: "이름 오름차순",
  });
  fireEvent.pointerDown(nameOption, { pointerType: "mouse" });
  fireEvent.click(nameOption);
  fireEvent.click(screen.getByRole("button", { name: "검색" }));

  const createdFrom = new Date(2026, 7, 1).toISOString();
  const createdTo = new Date(2026, 8, 1).toISOString();
  await waitFor(() => {
    expect(fileApi.getMyFiles).toHaveBeenLastCalledWith({
      name: "report",
      contentType: "application/pdf",
      createdFrom,
      createdTo,
      sort: "originalName",
      direction: "asc",
    });
  });
});

test("resets applied file filters", async () => {
  renderPage();
  await waitFor(() => expect(fileApi.getMyFiles).toHaveBeenCalled());
  fireEvent.change(screen.getByLabelText("파일 이름"), {
    target: { value: "report" },
  });
  fireEvent.click(screen.getByRole("button", { name: "검색" }));
  await waitFor(() =>
    expect(fileApi.getMyFiles).toHaveBeenLastCalledWith(
      expect.objectContaining({ name: "report" }),
    ),
  );

  fireEvent.click(screen.getByRole("button", { name: "초기화" }));

  await waitFor(() => {
    expect(fileApi.getMyFiles).toHaveBeenLastCalledWith({
      name: undefined,
      contentType: undefined,
      createdFrom: undefined,
      createdTo: undefined,
      sort: "createdAt",
      direction: "desc",
    });
  });
});

test("opens file details from the row action menu", async () => {
  fileApi.getMyFiles.mockResolvedValue({
    content: [storedFile],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  });
  renderPage();
  await screen.findByText("report.pdf");

  await chooseFileAction("상세 보기");

  const sheet = await screen.findByRole("dialog", { name: "파일 상세" });
  expect(within(sheet).getByText("application/pdf")).toBeInTheDocument();
  expect(within(sheet).getByText("2 KB")).toBeInTheDocument();
  expect(within(sheet).getByText("file-1")).toBeInTheDocument();
  fireEvent.click(within(sheet).getByRole("button", { name: "닫기" }));
  await waitFor(() =>
    expect(screen.queryByRole("dialog", { name: "파일 상세" })).toBeNull(),
  );
});

test("downloads a file from the row action menu", async () => {
  fileApi.getMyFiles.mockResolvedValue({
    content: [storedFile],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  });
  fileApi.downloadFile.mockResolvedValue(new Blob(["pdf"]));
  vi.stubGlobal("URL", {
    createObjectURL: vi.fn(() => "blob:file"),
    revokeObjectURL: vi.fn(),
  });
  vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
  renderPage();
  await screen.findByText("report.pdf");

  await chooseFileAction("다운로드");

  await waitFor(() => expect(fileApi.downloadFile).toHaveBeenCalledWith("file-1"));
});

test("confirms file deletion from the row action menu", async () => {
  fileApi.getMyFiles.mockResolvedValue({
    content: [storedFile],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  });
  fileApi.deleteFile.mockResolvedValue(undefined);
  renderPage();
  await screen.findByText("report.pdf");

  await chooseFileAction("삭제");

  const dialog = await screen.findByRole("alertdialog", {
    name: "파일을 삭제할까요?",
  });
  expect(within(dialog).getByText(/report\.pdf/)).toBeInTheDocument();
  fireEvent.click(within(dialog).getByRole("button", { name: "삭제" }));
  await waitFor(() => expect(fileApi.deleteFile.mock.calls[0][0]).toBe("file-1"));
});
