// FIXED — copy to:
//   frontend/src/components/PaymentForm.tsx
//
// The frontend uses ONLY the publishable key (designed to be public). The
// secret key never enters the client codebase. To create a charge the
// frontend posts to /api/payments/intent which uses the secret server-side.

const STRIPE_PUBLISHABLE = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;

export function PaymentForm() {
  console.log('Initializing Stripe with', STRIPE_PUBLISHABLE?.slice(0, 7), '...');

  async function pay() {
    // The backend uses the secret key. The frontend never sees it.
    await fetch('/api/payments/intent', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ amount: 1999 }),
    });
  }

  return <button onClick={pay}>Pay</button>;
}
