export function App() {
  return (
    <main style={{ fontFamily: 'system-ui', padding: '2rem', maxWidth: '720px' }}>
      <h1>Security Book — Reference App</h1>
      <p>
        This is the companion frontend. Each chapter folder under{' '}
        <code>chapters/</code> swaps in pages and components that demonstrate the
        vulnerability described in the book.
      </p>
      <p>
        Start with <a href="/login">/login</a> after running the backend on port
        8080.
      </p>
    </main>
  );
}
