import { api } from '../services/api';

// The top bar, the deal board and the home product groups all show the running offers.
// One request serves them; it is asked again after a minute so a long-open page does not go stale.
const MAX_AGE_MS = 60000;
let pending = null, startedAt = 0;

export function loadOffers() {
  if (!pending || Date.now() - startedAt > MAX_AGE_MS) {
    startedAt = Date.now();
    pending = api('/offers').catch((error) => { pending = null; throw error; });
  }
  return pending;
}
