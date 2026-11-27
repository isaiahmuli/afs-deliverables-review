import { useSuspenseQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'
import { workspaceQuery } from '../api/queries'

export default function WorkspaceDetail() {
  const { workspaceId } = useParams() as { workspaceId: string }
  // The route loader has already filled this query, so it renders straight away.
  const { data } = useSuspenseQuery(workspaceQuery(workspaceId))
  return (
    <section>
      <h2>{data.name}</h2>
      <p>{data.description}</p>
      <Link to="book">Book this workspace</Link>
    </section>
  )
}
