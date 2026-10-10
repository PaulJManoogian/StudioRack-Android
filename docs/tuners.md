# Instrument and Drum Tuners

Open **Music & Live > Tuners**. The Android tuner analyzes microphone audio locally. It does not record the session, upload audio, or send microphone content to Crew. Tuning continues without a network connection; a saved Gear update waits in the normal offline synchronization queue when necessary.

## Controls

| Control | What it does |
| --- | --- |
| Tuned Instruments / Percussion | Switches the standards and measurement workflow. Stopping or switching releases the current reading. |
| Standard tuning | Selects a known open-string arrangement such as Guitar Standard, Drop D, DADGAD, Open G, Bass, Ukulele, Violin/Mandolin, or Viola/Cello. |
| String / target | Chooses the particular string to tune. Chromatic mode follows the closest detected pitch. |
| Concert pitch | Sets A4 from 430 through 450 Hz. Use 440 Hz unless the ensemble or source requires another reference. |
| Drum standard | Selects a practical starting point for snares, rack toms, floor toms, bass drum, or timpani. |
| Measurement | Chooses whole drum, batter head, resonant head, or lug. Lug mode supplies positions 1 through 16. |
| Start Tuner | Requests microphone permission when needed, then begins local analysis. |
| Stop Tuner | Releases the microphone. Stop when finished so another app can use it. |
| Play Reference | Plays the selected target as a short local tone. Do not play the reference while measuring through the same microphone. |
| Pitch meter | Center is in tune, left is flat, and right is sharp. About three cents from center is displayed as in tune. |
| Confidence | Shows whether one stable pitch dominates the signal. Do not save a low-confidence result as a precise measurement. |
| Save tuning to... | Selects an existing Gear item and saves the target, measurement, zone, and notes. |

## Tune a pitched instrument

1. Select **Tuned Instruments**.
2. Choose the standard tuning. Use **Chromatic** for wind instruments, unlisted alternate tunings, or any sustained pitched source.
3. Choose the string or target note.
4. Confirm concert pitch.
5. Select **Start Tuner**, play one clear sustained note, and let the reading settle.
6. Adjust until the needle is centered and the display says **In tune**.
7. Optionally choose the physical instrument under **Save tuning to...**, add a note, and save.

## Tune a drum

The supplied values are useful starting points, not mandatory tunings for every shell, head, room, or musical style.

1. Select **Percussion** and the nearest drum standard.
2. Choose **Whole drum**, **Batter head**, **Resonant head**, or **Lug**.
3. For a head measurement, lightly damp the opposite head. For a whole-drum reading, let both heads respond.
4. For lugs, strike consistently near each tension rod and work around the head. Match lugs to one another before judging the whole drum.
5. Use a controlled strike. A very loud strike can clip a tablet microphone; a very soft strike can produce a weak result.
6. Wait for a clear confidence reading, then select the matching Gear item and save.

Batter and resonant measurements update their dedicated Gear tuning values. Whole-drum and lug measurements remain in the tuning history with their target, measured frequency, cents difference, and any notes. Timpani presets show starting ranges; tune to the score and verify pedal range mechanically.

## Reading problems

- **Signal is too quiet:** move the device closer, play or strike again, and reduce damping if appropriate.
- **Signal is clipping:** move the device farther away or use a softer strike.
- **Pitch is uncertain:** reduce room noise and sympathetic vibration, isolate one string or head, and try again.
- **Wrong octave or unstable note:** use a sustained tone, avoid touching the device microphone, and check that the selected target is appropriate.

Microphone quality and processing differ between Android devices. Verify critical studio measurements with known reference equipment. The tuner is isolated from Leviathan Live playback, the metronome, ChordPro timing, and the multitrack mixer.
