// FIXED — copy to:
//   frontend/src/pages/AdminPage.tsx
//
// The frontend guard is unchanged — it's still useful UX (don't show the
// menu link to non-admins, don't render the page if the role is wrong).
//
// The REAL fix lives on the server: AdminController carries
// @PreAuthorize("hasRole('ADMIN')"). See Chapter 11.
//
// The principle: every authorization decision must be enforced at the
// trust boundary (the API). Frontend checks are a UX optimization, never
// a security control.
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api } from '../lib/api';

interface Me {
  id: string;
  email: string;
  role: 'USER' | 'SELLER' | 'ADMIN';
}

export function AdminPage() {
  const navigate = useNavigate();
  const [users, setUsers] = useState<Me[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // UX-level guard: don't even attempt the call if role is wrong.
    api.get<Me>('/api/me').then((res) => {
      if (res.data.role !== 'ADMIN') {
        navigate('/forbidden');
        return;
      }
      // Server enforces the same rule via @PreAuthorize. Even if a
      // future regression removes the navigate() above, the API rejects.
      api.get<Me[]>('/api/admin/users')
        .then((r) => setUsers(r.data))
        .catch((e) => {
          if (e.response?.status === 403) {
            setError('Server denied the request — your role is not ADMIN.');
          } else {
            setError('Network error');
          }
        });
    });
  }, [navigate]);

  if (error) return <p>{error}</p>;

  return (
    <ul>
      {users.map((u) => (
        <li key={u.id}>{u.email} ({u.role})</li>
      ))}
    </ul>
  );
}
