import * as SecureStore from 'expo-secure-store';

import { createRecentSearchRepository } from '../features/ride-search';

const secureRecentSearchStorage = {
  getItemAsync: (key: string) => SecureStore.getItemAsync(key),
  setItemAsync: (key: string, value: string) => SecureStore.setItemAsync(key, value),
  deleteItemAsync: (key: string) => SecureStore.deleteItemAsync(key),
};

export const passengerRecentSearchRepository = createRecentSearchRepository(secureRecentSearchStorage);
