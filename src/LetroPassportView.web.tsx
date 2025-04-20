import * as React from 'react';

import { LetroPassportViewProps } from './LetroPassport.types';

export default function LetroPassportView(props: LetroPassportViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
