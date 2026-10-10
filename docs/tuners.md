# Instrument and Drum Tuners

Open **Music & Live > Tuners**. The Android tuner analyzes microphone audio locally. It does not record the session, upload audio, or send microphone content to Crew. Tuning continues without a network connection; a saved Gear update waits in the normal offline synchronization queue when necessary.

## Controls

| Control | What it does |
| --- | --- |
| Tuned / Percussion | Switches the standards and measurement workflow. Stopping or switching releases the current reading. |
| Standard tuning | Selects a known open-string arrangement such as Guitar Standard, Drop D, DADGAD, Open G, Bass, Ukulele, Violin/Mandolin, or Viola/Cello. |
| String / target | Chooses the particular string with a compact one-tap pitch button. Every string in the selected tuning remains visible beside the meter so you can move through an instrument without leaving the tuning view. Chromatic mode follows the closest detected pitch. |
| Concert pitch | Sets A4 from 430 through 450 Hz. Use 440 Hz unless the ensemble or source requires another reference. |
| Percussion type | Selects snare, tom, bass drum, timpani, djembe, conga, timbales, bongos, or tabla. |
| Size / voice | Selects the diameter or traditional voice within that family, such as quinto, tumba, macho, hembra, dayan, or bayan. |
| Measurement | Adapts to the selected instrument. Double-headed drums offer batter and resonant heads. Single-headed drums offer the playing head only. Djembe and tabla use tension-point terminology. |
| Listen / Stop | Starts or releases local microphone analysis. Stop when finished so another app can use the microphone. |
| Reference | Plays the selected target through the device's media-audio path. The fundamental stays exact while musical overtones keep low targets audible on compact phone and tablet speakers. Use the device media-volume control and do not play the tone while measuring through the same microphone. |
| Pitch meter | The segmented arc shows direction and distance: left is flat, right is sharp, and center is in tune. The detected pitch is red when substantially wrong, yellow when close, and green when exact. About three cents from center is displayed as in tune. |
| Confidence | Shows whether one stable pitch dominates the signal. Do not save a low-confidence result as a precise measurement. |
| Environment sensitivity | Studio, Quiet, Noisy, and Gig adjust the input gate, confidence requirement, smoothing, and practical in-tune tolerance. Use the quietest profile that remains stable. |
| Ambient input | Shows microphone level even when it is below the selected environment's detection gate. |
| Lock Current Reading | Makes the latest clear reading the working target for matching another head or drum. |
| Star / Clockwise | Selects the guided lug order. Star alternates opposing lugs; Clockwise visits adjacent lugs. |
| Drum head map | Orange is selected, green is in range, cyan is low, and coral is high. Select a numbered lug or use Previous and Next. |
| Capture Lug | Stores the clear reading for the selected lug, colors it on the map, and advances using the selected order. |
| Practical comparison | Shows typical fundamental, batter-lug, and resonant-lug ranges as both musical note spans and exact frequencies. These are comparison bands, not required tunings. |
| Musical range map | Places every supported size or voice in the selected percussion family on one chromatic pitch scale. The selected instrument is orange, making overlaps and useful registers easy to compare. |
| Save tuning to... | Selects an existing Gear item and saves the target, measurement, zone, and notes. |

## Tune a pitched instrument

1. Select **Tuned**.
2. Choose the standard tuning. Use **Chromatic** for wind instruments, unlisted alternate tunings, or any sustained pitched source.
3. Choose the string or target note.
4. Confirm concert pitch.
5. Select **Listen**, play one clear sustained note, and let the reading settle.
6. Adjust until the needle is centered and the display says **In tune**.
7. Optionally choose the physical instrument under **Save tuning to...**, add a note, and save.

## Tune a drum

The supplied values are useful starting points, not mandatory tunings for every shell, head, room, or musical style.

1. Select **Percussion**, the instrument family, and then its size or traditional voice.
2. Choose one of the measurements offered for that instrument. Resonant head appears only for double-headed drums; djembe and tabla use tension points instead of lugs.
3. Choose **Studio** in a controlled room, **Quiet** for ordinary practice, **Noisy** around nearby sound, or **Gig** on a loud stage. This filters ambient triggers; it does not alter pitch accuracy.
4. For a head measurement, lightly damp the opposite head. For a whole-drum reading, let both heads respond.
5. For lugs, hold the device microphone about 2 inches / 5 cm from the head, select **Star** while balancing the head, and strike consistently near the selected tension rod.
6. Wait for a clear confidence reading and choose **Capture Lug**. The map records high, low, or in-range state and advances to the next lug.
7. Compare the result with the note and frequency spans in the typical range panel. Use the Musical Range Map to compare the selected diameter with the other drums in that family. A musical choice outside the band is valid; shell, heads, interval, damping, and sustain determine the final sound.
8. Select the matching Gear item and save. The environment, lug order, and captured readings are included in the saved tuning history.

Batter and resonant measurements update their dedicated Gear tuning values. Whole-drum and lug measurements remain in the tuning history with their target, measured frequency, cents difference, and any notes. Timpani presets show starting ranges; tune to the score and verify pedal range mechanically.

## Reading problems

- **Signal is too quiet:** move the device closer, play or strike again, and reduce damping if appropriate.
- **Signal is clipping:** move the device farther away or use a softer strike.
- **Pitch is uncertain:** reduce room noise and sympathetic vibration, isolate one string or head, and try again.
- **Wrong octave or unstable note:** use a sustained tone, avoid touching the device microphone, and check that the selected target is appropriate.

Microphone quality and processing differ between Android devices. Verify critical studio measurements with known reference equipment. The tuner is isolated from Leviathan Live playback, the metronome, ChordPro timing, and the multitrack mixer.
