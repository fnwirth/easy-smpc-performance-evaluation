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
import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bihealth.mi.easybus.BusException;
import org.bihealth.mi.easybus.MessageListener;
import org.bihealth.mi.easybus.Scope;
import org.bihealth.mi.easybus.implementations.email.BusEmail;
import org.bihealth.mi.easybus.implementations.email.ConnectionIMAP;
import org.bihealth.mi.easybus.implementations.email.ConnectionIMAPSettings;

import de.tu_darmstadt.cbs.emailsmpc.Message;
import de.tu_darmstadt.cbs.emailsmpc.MessageInitial;
import de.tu_darmstadt.cbs.emailsmpc.Participant;

/**
 * A participant in an EasySMPC process
 * 
 * @author Felix Wirth
 * @author Fabian Prasser
 */
public class UserParticipating extends User {

    /** Logger */
    private static final Logger    LOGGER = LogManager.getLogger(UserParticipating.class);

    /** Connection settings */
    private ConnectionIMAPSettings connectionSettings;

    /**
     * Creates a new instance
     * 
     * @param initiator
     * @param participantID 
     */
    public UserParticipating(UserCreating initiator, int participantID) {                
   
        // Store
        super(initiator);
        
        // Settings
        connectionSettings = initiator.getMailboxSettings().getConnection(participantID);
        Participant participant = initiator.getModel().getParticipants()[participantID];
        
        // Init time recording
        getRecorder().addStartTime(participantID, System.nanoTime());
        
        try {
            // Register for initial e-mail
            BusEmail interimBus = new BusEmail(new ConnectionIMAP(connectionSettings, false),
                                               initiator.getMailboxCheckInterval());
            
            interimBus.receive(new Scope(initiator.getModel().getStudyUID() + ROUND_0),
                                        new org.bihealth.mi.easybus.Participant(participant.name,
                                                                                participant.emailAddress),
                                        new MessageListener() {
                                            @Override
                                            public void receive(String message) {
                                                // Stop interim bus
                                                interimBus.stop();
                                                
                                                // Spawns the following steps in an own thread
                                                Thread thread = new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        performInitialization(message);
                                                    }
                                                });
                                                thread.setDaemon(false);
                                                thread.start();
                                            }

                                            @Override
                                            public void receiveError(Exception e) {
                                                LOGGER.error("Error receiveing e-mails logged", new Date(), "Error receiving e-mails" ,ExceptionUtils.getStackTrace(e));
                                            }
                                        });
        } catch (BusException e) {
            LOGGER.error("Unable to register to receive initial e-mails logged", new Date(), "Unable to register to receive initial e-mails", ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException("Unable to register to receive initial e-mails", e);
        }
    }
    
    /**
     * Returns random decimals
     * @param num
     * @return
     */
    private BigDecimal[] createRandomDecimals(int num) {
        
        // Init
        BigDecimal[] result = new BigDecimal[num];
        
        // Set a random big integer for each
        for (int index = 0; index < result.length; index++) {
            result[index] = createRandomDecimal(FIXED_LENGTH_BIT_NUMBER);
        }
        
        // Return
        return result;
    }
    
    /**
     * Receives the initial e-mail
     * 
     * @param message
     */
    private void performInitialization(String message) {

        try {
            // Get data
            String data = Message.deserializeMessage(message).data;

            // Init model
            setModel(MessageInitial.getAppModel(MessageInitial.decodeMessage(Message.getMessageData(data))));
            getModel().setConnectionIMAPSettings(this.connectionSettings);

            // Proceed to entering value
            getModel().toEnteringValues(data);

            // Set own values and proceed
            getModel().toSendingShares(createRandomDecimals(getModel().getBins().length));

            // Starts the common steps 
            performCommonSteps();

        } catch (ClassNotFoundException | IllegalArgumentException | IllegalStateException | IOException e) {
            LOGGER.error("Unable to execute particpating users steps logged", new Date(), "Unable to execute particpating users steps", ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException("Unable to execute particpating users steps" , e);
        }        
    }
}