// In-memory chaos state for stress testing.
// failTarget: which transfer to route to the /fail endpoint — 'host' or 'guest'.
const chaosStore = {
  enabled:    false,
  failTarget: 'guest',
};

export default chaosStore;
