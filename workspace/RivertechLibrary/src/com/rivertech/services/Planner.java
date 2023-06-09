package com.rivertech.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import com.rivertech.model.Round;
import com.rivertech.model.Slot;

/***
 * The planner was designed for this project to schedule
 * games in slots without collisions.
 * @author Omar Zammit
 *
 */
public class Planner {

	final Logger logger = LogFactory.getConsoleHandler("Planner");

	/***
	 * Method used to determine if two rounds have collision elements.
	 * 
	 * @param a The first round to check.
	 * @param b The second round to check.
	 * @return True or false if there are collisions.
	 */
	public boolean hasCollision(Round a, Round b) {

		/* If the round is the same round, return collision */
		if (a.getId() == b.getId()) {
			return true;
		}

		/* Two games cannot happen at the same time */
		if (a.getGame().getGameId() == b.getGame().getGameId()) {
			return true;
		}

		/* Users cannot collide with each other */
		HashSet<String> aSet = new HashSet<String>(Arrays.asList(a.getUsers()));
		for (String userId : b.getUsers()) {
			if (aSet.contains(userId)) {
				return true;
			}
		}

		return false;
	}

	/***
	 * Given a list of rounds, the method tries to fit the rounds in different
	 * slots.
	 * 
	 * @param rounds The rounds to fit.
	 * @return Fitted slots.
	 */
	public List<Slot> fit(List<Round> rounds) {

		/* List of completed slots */
		List<Slot> fittedSlots = new ArrayList<>();

		/* List of fitted rounds */
		List<Round> fittedRounds = new ArrayList<>();

		/* Loop through each round and try to match non colliding rounds */
		for (Round roundA : rounds) {

			/* Check if the current round is already fitted */
			if (fittedRounds.contains(roundA)) {
				/* The round is already fitted */
				continue;
			}

			/* Create a slot */
			Slot slot = new Slot();
			/* Set the first round of the slot as round A */
			slot.setOne(roundA);
			/* Add roundA to the fitted list */
			fittedRounds.add(roundA);

			/* Find a non-colliding entry */
			for (Round roundB : rounds) {

				/* Check if the current round is already fitted */
				if (fittedRounds.contains(roundB)) {
					/* The round is already fitted */
					continue;
				}

				if (!hasCollision(roundA, roundB)) {
					/* Set the second round of the slot as round B */
					slot.setTwo(roundB);
					/* Add roundB to the fitted list */
					fittedRounds.add(roundB);
					break;
				}
			}

			/* Once done add the slot to the final collection */
			fittedSlots.add(slot);
		}
		/* Return fitted slots */
		return fittedSlots;
	}

	/***
	 * This method tries to find the best fit for a list of rounds. If the max slots
	 * are exceeded the method will return an empty list.
	 * 
	 * @param rounds   The rounds to fit.
	 * @param maxSlots The maximum slots
	 * @return The best fitted slots.
	 */
	public List<Slot> solve(List<Round> rounds, int maxSlots, LocalDateTime start) {

		List<Slot> slots = new ArrayList<>();

		/* Loop through each combination and create a fit */
		for (int i = 0; i < rounds.size(); i++) {
			Collections.swap(rounds, 0, i);
			/* Compute a fit */
			slots = fit(rounds);
			logger.fine("Planner fit iteration " + i + " has " + slots.size() + "slots.");
			if (slots.size() <= maxSlots)
				break;
		}

		/* Set the time slot start and end time */
		for (Slot slot : slots) {
			slot.setStart(start);
			start = start.plusHours(1);
			slot.setEnd(start);
		}

		/* Return the time slots */
		return slots;
	}

}
