import { PassengerAppProviders } from './src/application/providers';
import { RootShell } from './src/application/root-shell';

export default function App() {
  return (
    <PassengerAppProviders>
      <RootShell />
    </PassengerAppProviders>
  );
}
