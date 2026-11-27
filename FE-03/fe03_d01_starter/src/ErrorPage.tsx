import { isRouteErrorResponse, useRouteError } from 'react-router'
import { SiteHeader } from './Layout'

// Shown for an unmatched address and for any loader or action that throws.
export default function ErrorPage() {
  const error = useRouteError()
  const notFound = isRouteErrorResponse(error) && error.status === 404
  return (
    <main>
      <SiteHeader />
      <section role="alert">
        <h2>{notFound ? 'Page not found' : 'Something went wrong'}</h2>
        <p>
          {notFound
            ? 'There is nothing at this address. It may have been removed, or the link may be mistyped.'
            : 'Please try again.'}
        </p>
      </section>
    </main>
  )
}
