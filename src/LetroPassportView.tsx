import { requireNativeView } from 'expo';
import * as React from 'react';

import { LetroPassportViewProps } from './LetroPassport.types';

const NativeView: React.ComponentType<LetroPassportViewProps> =
  requireNativeView('LetroPassport');

export default function LetroPassportView(props: LetroPassportViewProps) {
  return <NativeView {...props} />;
}
