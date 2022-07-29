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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bihealth.mi.easysmpc.resources.Resources;

import de.tu_darmstadt.cbs.emailsmpc.Bin;
import de.tu_darmstadt.cbs.emailsmpc.Participant;

/**
 * A creating user in an EasySMPC process
 * 
 * @author Felix Wirth
 * @author Fabian Prasser
 */
public class UserCreating extends User {

    /** Logger */
    private static final Logger LOGGER             = LogManager.getLogger(UserCreating.class);
 
    /** Participating users */
    private List<User>          participatingUsers = new ArrayList<>();

    /**
     * Create a new instance
     * 
     * @param numberParticipants
     * @param numberBins
     * @param mailboxSettings
     * @param separatedProcesses 
     * @throws IllegalStateException
     */
    public UserCreating(int numberParticipants,
                        int numberBins,
                        int mailboxCheckInterval,
                        PerformanceMailboxSettings mailboxSettings,
                        PerformanceResultPrinter printer) throws IllegalStateException {
        
        super(mailboxCheckInterval, mailboxSettings, printer);

        try {          
            // Set model to starting
            getModel().toStarting();          
            
            // Init model with generated study name, participants and bins 
            getModel().toInitialSending(createRandomString(FIXED_LENGTH_STRING),
                                        createParticpants(numberParticipants, mailboxSettings, FIXED_LENGTH_STRING),
                                        createBins(numberBins,numberParticipants, FIXED_LENGTH_STRING), mailboxSettings.getConnection(0));
            // Init recording
            getRecorder().addStartTime(getModel().getOwnId(), System.nanoTime());
            LOGGER.debug("Started", new Date(), getModel().getStudyUID(), "started", getModel().getNumParticipants(), "participants", getModel().getBins().length, "bins", mailboxCheckInterval, "mailbox check interval");
            
        } catch (IOException | IllegalStateException e) {
            LOGGER.error("Unable to init logged", new Date(), "Unable to init", ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException("Unable to init study!", e);
        }
        
        // Spawn participants
        participatingUsers = createParticipatingUsers(FIXED_LENGTH_BIT_NUMBER, mailboxSettings);
        
        // Spawns the common steps in an own thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                performCommonSteps();
            }
        }).start();             
    }

    /**
     * Are all users finished?
     * 
     * @return
     */
    public boolean areAllUsersFinished() {        
        
        // Check for this creating users
        if (!isProcessFinished()) {
            return false;
        }
        
        // Check for all participating users
        for(User user : participatingUsers) {
            if(!user.isProcessFinished()) {
                return false;
            }
        }        
        
        // Return all
        return true;
    }

    /**
     * Generate bins
     * 
     * @param numberBins number of bins
     * @param numberParties number of involved parties/users
     * @param stringLength length of bin name
     * @return
     */
    private Bin[] createBins(int numberBins, int numberParties, int stringLength) {

        // Init result bin array
        Bin[] result = new Bin[numberBins];
        
        // Init each bin and set generated secret value of creating user
        for (int index = 0; index < numberBins; index++) {
            result[index] = new Bin(createRandomString(stringLength), numberParties);
            result[index].shareValue(createRandomDecimal(FIXED_LENGTH_BIT_NUMBER), Resources.FRACTIONAL_BITS);
        }
        
        // Return
        return result;
    }


    /**
     * Create participants
     * 
     * @param bitLengthNumber
     * @param separatedProcesses
     * @param connectionIMAPSettings 
     */
    private List<User> createParticipatingUsers(int bitLengthNumber,
                                                PerformanceMailboxSettings mailBoxDetails) {

        // Result
        List<User> result = new ArrayList<>();
        
        // Create
        for(int index = 1; index < getModel().getNumParticipants(); index++) {
            result.add(new UserParticipating(this, index));
        }
        
        // Done
        return result;
    }
    
    /**
     * Generate the involved participants
     * 
     * @param numberParticipants
     * @param connectionIMAPSettings 
     * @param stringLength length of names and e-mail address parts
     * @return
     */
    private Participant[] createParticpants(int numberParticipants, PerformanceMailboxSettings mailBoxDetails, int stringLength) {

        // Init result
        Participant[] result = new Participant[numberParticipants];
        
        // Init each participant with a generated name and generated mail address
        for(int index = 0; index < numberParticipants; index++) {            
            
            // Create participant   
            result[index] = new Participant(createRandomString(15), mailBoxDetails.getConnection(index).getIMAPEmailAddress());
        }
        
        // Return
        return result;
    }
    
    /**
     * Generates a string with random letters
     * 
     * @param string length
     * @return generated string
     */
    protected String createRandomString(int stringLength) {   
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < stringLength; i++) {
            b.append((char) (Math.random() * 26 + 'a'));
        }
        return b.toString();
    }
}