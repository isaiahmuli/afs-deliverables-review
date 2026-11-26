export type Workspace = { id: string; name: string; city: string; available: boolean }
export type WorkspacePage = { items: Workspace[]; page: number; totalPages: number }
export type Booking = {
  id: string
  workspaceId: string
  workspaceName: string
  date: string
}
export type HistoryPage = { items: Booking[]; nextCursor: string | null }
