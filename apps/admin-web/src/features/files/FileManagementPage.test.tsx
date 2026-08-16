import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
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

afterEach(cleanup);

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
