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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bihealth.mi.easybus.BusException;
import org.bihealth.mi.easybus.implementations.email.BusEmail;
import org.bihealth.mi.easybus.implementations.email.ConnectionIMAP;
import org.bihealth.mi.easybus.implementations.email.ConnectionIMAPSettings;
import org.bihealth.mi.easysmpc.performanceevaluation.Combinator.Combination;

/**
 * EasySMPC performance evaluation
 * 
 * @author Felix Wirth
 * @author Fabian Prasser
 */
public class Main {
    
	/** File path */
	private static final String FILEPATH = "result.csv";
	
	/** Logger */
	private static final Logger LOGGER = LogManager.getLogger(Main.class);
	
	/**
	 * Starts the performance test
	 *
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
	    
	    // Output start message
        System.out.println("Performance evaluation started");
        System.out.println("For results see /root/easy-smpc/"+ FILEPATH);
        
        PerformanceResultPrinter printer = new PerformanceResultPrinter(FILEPATH);
        
        // Repetitions
        int repetitionsPerCombination = 15;
        
		// Performance tracking
		PerformanceTracker tracker = new PerformanceTracker();

        // Create connection settings
        ConnectionIMAPSettings connectionIMAPSettings = new ConnectionIMAPSettings(
                "easy" + PerformanceMailboxSettings.INDEX_REPLACE + "@easysmpc.org", null).setIMAPPassword("12345").setSMTPPassword("12345")
                        .setSMTPServer("localhost").setIMAPServer("localhost").setIMAPPort(993).setSMTPPort(465)
                        .setAcceptSelfSignedCertificates(true).setSearchForProxy(false).setPerformanceListener(tracker);

		// Create parameters
		List<Integer> participants = new ArrayList<>(Arrays.asList(new Integer[] { 20, 15, 10, 5, 3 }));
		List<Integer> bins = new ArrayList<>(Arrays.asList(new Integer[] { 10000, 7500, 5000, 2500, 1000 }));
		List<Integer> mailboxCheckInterval = new ArrayList<>(Arrays.asList(new Integer[] { 20000, 15000, 10000, 5000, 1000 }));
		
		// Create combinator
		Combinator combinator = new CombinatorRepeatPermute(participants, bins, mailboxCheckInterval, repetitionsPerCombination);

		// Create mailbox details
		PerformanceMailboxSettings mailboxSettings = new PerformanceMailboxSettings(connectionIMAPSettings, participants, tracker);

        // Prepare mailbox
        try {

            // Delete existing e-mails relevant to EasySMPC
            for (ConnectionIMAPSettings imapConnectionSettings : mailboxSettings.getAllConnections()) {
                BusEmail bus = new BusEmail(new ConnectionIMAP(imapConnectionSettings, false), 1000);
                bus.purgeEmails();
                bus.stop();
            }

        } catch (BusException | InterruptedException e) {
            LOGGER.error("Preparation failed logged", new Date(), "Preparation failed", ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException("Unable to prepare performance evaluation", e);
        }

        // Conduct an EasySMPC process over each combination
        for(Combination combination: combinator) {

            // Start an EasySMPC process
            UserCreating user = new UserCreating(combination.getParticipants(), 
                                                 combination.getBins(), 
                                                 combination.getMailboxCheckInterval(), 
                                                 mailboxSettings,
                                                 printer);

            // Wait to finish
            while (!user.areAllUsersFinished()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted exception logged", new Date(), "Interrupted exception logged",
                                 ExceptionUtils.getStackTrace(e));
                }
            }

            // Reset statistics
            mailboxSettings.getTracker().resetStatistics();

            // Wait
            LOGGER.debug("Process finished logged", new Date(), "Process finished!");
        }
        
        // Close printer
        printer.close();
	}
}