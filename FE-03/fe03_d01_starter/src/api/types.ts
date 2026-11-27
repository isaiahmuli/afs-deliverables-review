export type Workspace = { id: string; name: string; city: string; description: string }
export type WorkspaceItem = Workspace & { available: boolean }
export type WorkspacePage = { items: WorkspaceItem[]; page: number; totalPages: number }
export type Booking = {
  id: string
  workspaceId: string
  workspaceName: string
  date: string
  name: string
  email: string
  status: 'confirmed' | 'cancelled'
}
export type NewBooking = { workspaceId: string; date: string; name: string; email: string }
export type HistoryPage = { items: Booking[]; nextCursor: string | null }
