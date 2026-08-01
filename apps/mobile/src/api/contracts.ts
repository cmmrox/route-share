import { mobileApiEndpoints, mobileApiLiveEndpoints } from '@routeshare/api-contracts';

/**
 * One app, one contract (Decision 011). `/passenger/**` and `/driver/**` are role-scoped resource
 * namespaces, not app boundaries, so both live in the same inventory.
 */
export const mobileContractEndpoints = mobileApiEndpoints;

/** Every path in the contract, including those a later slice will build. */
export const mobileContractPaths = mobileApiEndpoints.map((endpoint) => `${endpoint.method} ${endpoint.path}`);

/**
 * Paths the app may actually call today. Anything outside this set is specified but not yet
 * implemented — calling it is a wiring bug, not a runtime condition to handle.
 */
export const mobileLiveContractPaths = mobileApiLiveEndpoints.map((endpoint) => `${endpoint.method} ${endpoint.path}`);

export type MobileContractPath = (typeof mobileContractPaths)[number];
