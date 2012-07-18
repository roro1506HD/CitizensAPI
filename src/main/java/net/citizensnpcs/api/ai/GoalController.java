package net.citizensnpcs.api.ai;

/**
 * Represents a collection of goals that are prioritised and executed, allowing
 * behaviour trees via a {@link GoalSelector}.
 */
public interface GoalController extends Runnable {
    /**
     * Registers a {@link Goal} with a given priority. Priority must be greater
     * than 0.
     * 
     * @param priority
     *            The goal priority
     * @param goal
     *            The goal
     */
    void addGoal(Goal goal, int priority);

    /**
     * Removes a {@link Goal} from rotation.
     * 
     * @param goal
     *            The goal to remove
     */
    void removeGoal(Goal goal);
}
