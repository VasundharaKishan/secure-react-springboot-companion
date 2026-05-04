// VULNERABLE — copy to:
//   frontend/src/components/CommentList.tsx
//
// dangerouslySetInnerHTML drops React's automatic escaping. Combined with
// the vulnerable backend (which doesn't sanitize), any HTML the attacker
// posted into a comment runs in every viewer's browser.
import type { Comment } from '../types/comment';

export function CommentList({ comments }: { comments: Comment[] }) {
  return (
    <ul>
      {comments.map((c) => (
        <li key={c.id}>
          <strong>{c.authorEmail}</strong>{' '}
          {/* BUG: rendering raw HTML. The attacker controls c.body. */}
          <span dangerouslySetInnerHTML={{ __html: c.body }} />
        </li>
      ))}
    </ul>
  );
}
