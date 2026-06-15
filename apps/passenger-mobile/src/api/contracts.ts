import { passengerApiEndpoints } from '@routeshare/api-contracts';

export const passengerContractEndpoints = passengerApiEndpoints;
export const passengerContractPaths = passengerApiEndpoints.map((endpoint) => `${endpoint.method} ${endpoint.path}`);

export type PassengerContractPath = (typeof passengerContractPaths)[number];
