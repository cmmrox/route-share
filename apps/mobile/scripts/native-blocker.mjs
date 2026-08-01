const target = process.argv[2] ?? 'native automation';
console.error('Blocked: ' + target + ' requires Expo/native scaffold and device automation, which are intentionally out of scope for Task 01 typed API client.');
process.exit(1);
