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
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bihealth.mi.easybus.BusException;
import org.bihealth.mi.easybus.MessageListener;
import org.bihealth.mi.easybus.Scope;
import org.bihealth.mi.easysmpc.dataimport.ImportClipboard;
import org.bihealth.mi.easysmpc.resources.Resources;

import de.tu_darmstadt.cbs.emailsmpc.Bin;
import de.tu_darmstadt.cbs.emailsmpc.Message;
import de.tu_darmstadt.cbs.emailsmpc.Study;
import de.tu_darmstadt.cbs.emailsmpc.Study.StudyState;

/**
 * A user in an EasySMPC process
 * 
 * @author Felix Wirth
 * @author Fabian Prasser
 */
public abstract class User implements MessageListener {

    /** Logger */
    private static final Logger        LOGGER                  = LogManager.getLogger(User.class);
    /** The length of a generated string */
    public static final int            FIXED_LENGTH_STRING     = 10;
    /** The length of a generated number before the point */
    public static final int            FIXED_LENGTH_BIT_NUMBER = 31;
    /** Round for initial e-mails */
    public static final String         ROUND_0                 = "_round0";
    
    /** The study model */
    private Study                      model                   = new Study();
    /** The random object */
    private final SecureRandom         randomGenerator         = new SecureRandom();
    /** The mailbox check interval in milliseconds */
    private final int                  mailBoxCheckInterval;
    /** Is shared mailbox used? */
    private PerformanceMailboxSettings mailboxSettings;
    /** Store the time differences */
    private PerformanceRecorder        recording;
    /** Printer */
    private PerformanceResultPrinter   printer;

    /**
     * Creates a new instance for creating users
     * 
     * @param mailboxCheckInterval
     * @param mailboxSettings 
     * @param printer
     */
    public User(int mailboxCheckInterval,
                PerformanceMailboxSettings mailboxSettings,
                PerformanceResultPrinter printer) {
        
        this.mailBoxCheckInterval = mailboxCheckInterval;
        this.mailboxSettings = mailboxSettings;
        this.printer = printer;
        this.recording = new PerformanceRecorder(model);
     }

    /**
     * Creates a new instance for participating users
     * 
     * @param other
     */
    public User(User other) {
        
        this.mailBoxCheckInterval = other.mailBoxCheckInterval;
        this.mailboxSettings = other.mailboxSettings;
        this.printer = other.printer;
        this.recording = other.recording;
     }
    
    /**
     * @return the mailBoxCheckInterval
     */
    public int getMailboxCheckInterval() {
        return mailBoxCheckInterval;
    }
    
    /**
     * Returns the mailbox settings
     * @return
     */
    public PerformanceMailboxSettings getMailboxSettings() {
        return mailboxSettings;
    }
    
    /**
     * Gets the model
     * 
     * @return the model
     */
    public Study getModel() {
        return model;
    }


    /**
     * Returns the recording
     * 
     * @return
     */
    public PerformanceRecorder getRecorder() {
        return recording;
    }

    /**
     * Is the process finished?
     * 
     * @return
     */
    public boolean isProcessFinished() {
        return getModel().getState() == StudyState.FINISHED;
    }
    
    @Override
    public void receive(String message) {
        String messageStripped = ImportClipboard.getStrippedExchangeMessage(message);
        
        // Check if valid
        if (isMessageShareResultValid(messageStripped)) {
            try {
                // Set message
                model.setShareFromMessage(Message.deserializeMessage(messageStripped));
            } catch (IllegalStateException | IllegalArgumentException | NoSuchAlgorithmException | ClassNotFoundException | IOException e) {
                LOGGER.error("Unable to digest message logged", new Date(), "Unable to digest message", ExceptionUtils.getStackTrace(e));
            }
        }
    }
    
    @Override
    public void receiveError(Exception e) {
        LOGGER.error("Error receiveing e-mails logged",
                     new Date(),
                     "Error receiveing e-mails",
                     ExceptionUtils.getStackTrace(e));
    }

    /**
     * Are shares complete to proceed?
     * 
     * @return
     */
    private boolean areSharesComplete() {
        for (Bin b : this.model.getBins()) {
            if (!b.isComplete()) return false;
        }
        return true;
    }
  
    /**
     * Check whether message is valid
     * 
     * @param text
     * @return
     */
    private boolean isMessageShareResultValid(String text) {
        // Check not null or empty
        if (model == null || text == null || text.trim().isEmpty()) { return false; }

        // Check message
        try {
            return model.isMessageShareResultValid(Message.deserializeMessage(text));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Prints the results
     */
    private void printResults() {

        // Write performance results
        try {
            printer.print(new Date(),
                          model.getStudyUID(),
                          model.getNumParticipants(),
                          model.getBins().length,
                          mailBoxCheckInterval,
                          recording.getFastest(),
                          recording.getSlowest(),
                          recording.getMean(),
                          mailboxSettings.getTracker().getNumberMessagesReceived(),
                          mailboxSettings.getTracker().getTotalSizeMessagesReceived(),
                          mailboxSettings.getTracker().getNumberMessagesSent(),
                          mailboxSettings.getTracker().getTotalsizeMessagesSent());
            
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write performance results", e);
        }
        
        // Fastest finished entry => log
        LOGGER.debug("Entry logged",
                     new Date(),
                     model.getStudyUID(),
                     "finished",
                     "first",
                     recording.getFastest(),
                     "duration");

        // Slowest finished entry => log
        LOGGER.debug("Slowest entry logged",
                     new Date(),
                     model.getStudyUID(),
                     "finished",
                     "last",
                     recording.getSlowest(),
                     "duration",
                     recording.getMean());
    }
    
    /**
     * Receives a message by means of e-mail bus
     * 
     * @param roundIdentifier
     * @throws IllegalArgumentException
     * @throws BusException
     */
    private void receiveMessages(String roundIdentifier) throws IllegalArgumentException, BusException {
        getModel().getBus(this.mailBoxCheckInterval, false).receive(new Scope(getModel().getStudyUID() + roundIdentifier),
                           new org.bihealth.mi.easybus.Participant(getModel().getParticipantFromId(getModel().getOwnId()).name,
                                                                   getModel().getParticipantFromId(getModel().getOwnId()).emailAddress),
                           this);
        
        // Wait for all shares
        while (!areSharesComplete()) {

            // Proceed if shares complete
            if (!getModel().isBusAlive()) {
                LOGGER.error("Bus is not alive anymore!", new Date(), "Bus is not alive anymore!");
            }
            
            // Wait
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted exception logged", new Date(), "Interrupted exception logged", ExceptionUtils.getStackTrace(e));
            }             
        }     
    }
    
    /** 
     * Sends a message by means of e-mail bus
     * 
     * @param roundIdentifier
     */
    private void sendMessages(String roundIdentifier) {
        
        // Loop over participants
        for (int index = 0; index < getModel().getNumParticipants(); index++) {
            
            // Only proceed if not own user
            if (index != getModel().getOwnId()) {

                try {
                    // Retrieve bus and send message
                    FutureTask<Void> future = getModel().getBus(this.mailBoxCheckInterval, false).send(Message.serializeMessage(getModel().getUnsentMessageFor(index)),
                                    new Scope(getModel().getStudyUID() + (getModel().getState() == StudyState.INITIAL_SENDING ? ROUND_0 : roundIdentifier)),
                                    new org.bihealth.mi.easybus.Participant(getModel().getParticipants()[index].name,
                                                                            getModel().getParticipants()[index].emailAddress));
                    
                    // Wait for result with a timeout time
                    future.get(Resources.TIMEOUT_SEND_EMAILS, TimeUnit.MILLISECONDS);
                    
                    // Mark message as sent
                    model.markMessageSent(index);
                } catch (Exception e) {
                    LOGGER.error("Unable to send e-mail logged", new Date(), "Unable to send e-mail", ExceptionUtils.getStackTrace(e));
                    throw new IllegalStateException("Unable to send e-mail!", e);
                }
            }
        }
    }

    /**
     * Generates a random big decimal
     * 
     * @param bit length of big decimal before comma
     * @return
     * @throws IllegalArgumentException
     */
    protected BigDecimal createRandomDecimal(int bitLength) throws IllegalArgumentException {
        
        // Check
        if (bitLength < 2) throw new IllegalArgumentException("Bitlength must be larger than 2");
        
        // Random integer
        BigDecimal value =  new BigDecimal(new BigInteger(bitLength, randomGenerator));
        
        // Swap sign? 
        byte[] randomByte = new byte[1];
        randomGenerator.nextBytes(randomByte);
        int signum = Byte.valueOf(randomByte[0]).intValue() & 0x01;
        if (signum == 1) value = value.negate();
        
        // Return
        return value;
      }
 
    /**
     * Proceeds the SMPC steps which are the same for participating and creating user
     */
    protected void performCommonSteps() {
        
        try {
            // Sends the messages for the first round and proceeds the model
            sendMessages(Resources.ROUND_1);
            this.model.toRecievingShares();
            LOGGER.debug("1. round sending finished logged", new Date(), getModel().getStudyUID(), "1. round sending finished for", getModel().getOwnId());
            
            // Receives the messages for the first round and proceeds the model
            receiveMessages(Resources.ROUND_1);
            this.model.toSendingResult();
            LOGGER.debug("1. round receiving finished logged", new Date(), getModel().getStudyUID(), "1. round receiving finished for", getModel().getOwnId());
            
            // Sends the messages for the second round and proceeds the model
            sendMessages(Resources.ROUND_2);
            this.model.toRecievingResult();
            LOGGER.debug("2. round sending finished logged", new Date(), getModel().getStudyUID(), "2. round sending finished for", getModel().getOwnId());
            
            // Receives the messages for the second round, stops the bus and finalizes the model
            receiveMessages(Resources.ROUND_2);
            getModel().stopBus();
            recording.addFinishedTime(this.model.getOwnId(), System.nanoTime());
            if (recording.isDone()) {
                printResults();
            }
            this.model.toFinished();
            LOGGER.debug("Result logged", new Date(), getModel().getStudyUID(), "result", getModel().getOwnId(), "participantid", getModel().getAllResults()[0].name, "result name", getModel().getAllResults()[0].value, "result");
            
        } catch (IllegalStateException | IllegalArgumentException | IOException | BusException e) {
            LOGGER.error("Unable to process common process steps logged", new Date(), "Unable to process common process steps", ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException("Unable to process common process steps", e);
        }
    }

    /**
     * Sets the model
     * 
     * @param the model
     */
    protected void setModel(Study model) {
        this.model = model;
    }
}