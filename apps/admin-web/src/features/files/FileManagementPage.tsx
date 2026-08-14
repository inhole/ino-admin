import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type ChangeEvent } from 'react'
import { ApiClientError, deleteFile, downloadFile, getMyFiles, uploadFile, type StoredFileSummary } from '@/api/client'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'

function fileSize(bytes: number) { return bytes < 1024 ? `${bytes} B` : bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB` }

export function FileManagementPage() {
  const queryClient = useQueryClient(); const files = useQuery({ queryKey: ['files'], queryFn: getMyFiles })
  const [error, setError] = useState<string | null>(null); const [message, setMessage] = useState<string | null>(null)
  const upload = useMutation({ mutationFn: uploadFile, onSuccess: async () => { setError(null); setMessage('파일을 업로드했습니다.'); await queryClient.invalidateQueries({ queryKey: ['files'] }) }, onError: (caught) => setError(caught instanceof ApiClientError ? caught.message : '파일을 업로드할 수 없습니다.') })
  const remove = useMutation({ mutationFn: deleteFile, onSuccess: async () => { setError(null); setMessage('파일을 삭제했습니다.'); await queryClient.invalidateQueries({ queryKey: ['files'] }) }, onError: (caught) => setError(caught instanceof ApiClientError ? caught.message : '파일을 삭제할 수 없습니다.') })
  const select = (event: ChangeEvent<HTMLInputElement>) => { const file = event.target.files?.[0]; setMessage(null); if (file) upload.mutate(file); event.target.value = '' }
  const download = async (file: StoredFileSummary) => { try { const blob = await downloadFile(file.id); const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = file.originalName; link.click(); URL.revokeObjectURL(url) } catch (caught) { setError(caught instanceof ApiClientError ? caught.message : '파일을 다운로드할 수 없습니다.') } }
  return <><header className="mb-8"><p className="text-xs font-semibold tracking-[0.2em] text-primary">STORAGE</p><h1 className="text-3xl font-bold tracking-tight">파일 관리</h1></header>
    <Card className="mb-6"><CardHeader><CardTitle>파일 업로드</CardTitle><CardDescription>PDF, PNG, JPEG, 텍스트 파일을 최대 10MB까지 업로드할 수 있습니다.</CardDescription></CardHeader><CardContent><Input aria-label="업로드할 파일" accept=".pdf,.png,.jpg,.jpeg,.txt" disabled={upload.isPending} onChange={select} type="file" />{upload.isPending && <p className="mt-3 text-sm" role="status">업로드 중…</p>}</CardContent></Card>
    {error && <Alert className="mb-4" variant="destructive" role="alert"><AlertDescription>{error}</AlertDescription></Alert>}{message && <p className="mb-4 text-sm text-emerald-700" role="status">{message}</p>}
    <Card><CardHeader><CardTitle>내 파일</CardTitle><CardDescription>본인이 업로드한 파일만 표시됩니다.</CardDescription></CardHeader><CardContent>{files.isPending && <p role="status">파일을 불러오는 중…</p>}{files.isError && <Alert variant="destructive" role="alert"><AlertDescription>파일 목록을 불러올 수 없습니다.</AlertDescription></Alert>}{files.data?.content.length === 0 && <p>업로드한 파일이 없습니다.</p>}{files.data && files.data.content.length > 0 && <div className="grid gap-2">{files.data.content.map(file => <div className="flex items-center justify-between gap-4 rounded-md border p-3" key={file.id}><div className="min-w-0"><p className="truncate font-medium">{file.originalName}</p><p className="text-xs text-muted-foreground">{fileSize(file.size)} · {new Date(file.createdAt).toLocaleString('ko-KR')}</p></div><div className="flex gap-2"><Button onClick={() => download(file)} size="sm" variant="outline">다운로드</Button><Button disabled={remove.isPending} onClick={() => remove.mutate(file.id)} size="sm" variant="outline">삭제</Button></div></div>)}</div>}</CardContent></Card>
  </>
}
