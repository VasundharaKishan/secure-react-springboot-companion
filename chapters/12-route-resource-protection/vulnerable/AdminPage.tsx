// VULNERABLE — copy to:
//   frontend/src/pages/AdminPage.tsx
//
// The bug: protection is enforced ONLY in the browser. The component
// checks the user's role from React state and redirects if not admin.
// If the API itself doesn't check (see vulnerable AdminController in
// chapters/11-authorization-fundamentals/), an attacker who skips the
// React app entirely — by hitting /api/admin/users with curl — gets the
// data anyway.
//
// The frontend guard prevents an honest user from clicking the wrong
// link. It is not a security control.
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

  useEffect(() => {
    // BUG: this is the ONLY check. A determined caller bypasses it by
    // simply not running the React app.
    api.get<Me>('/api/me').then((res) => {
      if (res.data.role !== 'ADMIN') {
        navigate('/forbidden');
        return;
      }
      api.get<Me[]>('/api/admin/users').then((r) => setUsers(r.data));
    });
  }, [navigate]);

  return (
    <ul>
      {users.map((u) => (
        <li key={u.id}>{u.email} ({u.role})</li>
      ))}
    </ul>
  );
}
