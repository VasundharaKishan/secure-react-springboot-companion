// FIXED — copy to:
//   frontend/src/components/CommentList.tsx
//
// Two changes from the vulnerable version:
//   1. No dangerouslySetInnerHTML. Plain text rendering means React escapes
//      every character.
//   2. If we genuinely needed limited HTML (e.g. <b>, <i>), we would route
//      it through DOMPurify with a strict allowlist — never a hand-rolled
//      regex.
import type { Comment } from '../types/comment';

export function CommentList({ comments }: { comments: Comment[] }) {
  return (
    <ul>
      {comments.map((c) => (
        <li key={c.id}>
          <strong>{c.authorEmail}</strong> {c.body}
        </li>
      ))}
    </ul>
  );
}
