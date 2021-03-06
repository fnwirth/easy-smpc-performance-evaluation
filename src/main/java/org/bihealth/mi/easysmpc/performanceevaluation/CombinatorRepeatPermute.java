/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bihealth.mi.easysmpc.performanceevaluation;

import java.util.Iterator;
import java.util.List;

/**
 * A class to permute the parameters and repeat them
 * 
 * @author Felix Wirth
 * @author Fabian Prasser
 */
public class CombinatorRepeatPermute extends Combinator {

    /** How often repeat each step? */
    private final int repeatPerCombination;
    /** Index of participant */
    private int       particpantIndex           = 0;
    /** Index of bin */
    private int       binIndex                  = 0;
    /** Index of mailboxCheckInterval */
    private int       mailboxCheckIntervalIndex = 0;
    /** Count repetitions per combination */
    private int       counterRepetition         = 0;

    /**
     * Creates a new instance
     * 
     * @param participants
     * @param bins
     * @param mailboxCheckInterval
     */
    public CombinatorRepeatPermute(List<Integer> participants,
                                   List<Integer> bins,
                                   List<Integer> mailboxCheckInterval,
                                   int repeatPerStep) {
        super(participants, bins, mailboxCheckInterval);

        // Store
        this.repeatPerCombination = repeatPerStep;
    }

    @Override
    public boolean hasNext() {
        return next(true);
    }

    @Override
    public Iterator<Combination> iterator() {
        return this;
    }

    @Override
    public Combination next() {
        if (!next(false)) { return null; }

        // Return
        return new Combination(getParticipants().get(particpantIndex),
                               getBins().get(binIndex),
                               getMailboxCheckInterval().get(mailboxCheckIntervalIndex));
    }

    /**
     * Increases the counter and indexes
     * 
     * @param dryRun If actually increase numbers, if false just check if possible
     * @return Is there a next element?
     */
    private boolean next(boolean dryRun) {

        // Copy fields as local variables
        int mailboxCheckIntervalIndex = this.mailboxCheckIntervalIndex;
        int binIndex = this.binIndex;
        int particpantIndex = this.particpantIndex;
        int counterRepetition = this.counterRepetition;

        // Advance indexes if necessary
        if (this.counterRepetition >= this.repeatPerCombination) {

            // Reset counter
            counterRepetition = 0;

            // Increase mailbox check interval index
            boolean increase = false;
            if (mailboxCheckIntervalIndex + 1 >= getMailboxCheckInterval().size()) {
                mailboxCheckIntervalIndex = 0;
                increase = true;
            } else {
                mailboxCheckIntervalIndex++;
            }

            // Increase bin index
            if (increase) {
                if (binIndex + 1 >= getBins().size()) {
                    binIndex = 0;
                    increase = true;
                } else {
                    ++binIndex;
                    increase = false;
                }
            }

            // Increase participants index
            if (increase) {
                if (particpantIndex + 1 >= getParticipants().size()) {
                    // Return false only if no more participants
                    return false;
                } else {
                    ++particpantIndex;
                }
            }
        }

        // Increase counter
        ++counterRepetition;

        // If actual run store variables
        if (!dryRun) {
            // Store counter
            this.counterRepetition = counterRepetition;
            // Store indexes
            this.mailboxCheckIntervalIndex = mailboxCheckIntervalIndex;
            this.binIndex = binIndex;
            this.particpantIndex = particpantIndex;
        }

        return true;
    };
}
