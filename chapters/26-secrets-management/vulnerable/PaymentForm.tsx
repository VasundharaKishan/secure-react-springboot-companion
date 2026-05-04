// VULNERABLE — copy to:
//   frontend/src/components/PaymentForm.tsx
//
// Reading the SECRET key from import.meta.env at module load. Vite inlines
// the value at build time, so the production bundle contains a literal
// string equal to the secret. Anyone who opens DevTools, downloads the
// JS bundle, or runs `curl https://app/assets/index-*.js` can read it.

const STRIPE_SECRET = import.meta.env.VITE_STRIPE_SECRET_KEY;

export function PaymentForm() {
  // BUG: the literal string `sk_live_...` appears in the bundle that
  // ships to every browser.
  console.log('Initializing Stripe with', STRIPE_SECRET?.slice(0, 7), '...');

  return <button>Pay</button>;
}
