package joy.multithreading;

import java.util.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        final TaskGroup group1 = new TaskGroup(new UUID(1, 2));
        final TaskGroup group2 = new TaskGroup(new UUID(1, 2));

        Task task1 = new Task(new UUID(1, 4), group1, TaskType.READ, new Callable() {
            @Override
            public String call() throws Exception {
                return "returning from 'Task 1'";
            }
        });
        Task task2 = new Task(new UUID(1, 4), group1, TaskType.WRITE, new Callable() {
            @Override
            public String call() throws Exception {
                return "returning from 'Task 2'";
            }
        });
        Task task3 = new Task(new UUID(1, 4), group2, TaskType.READ, new Callable() {
            @Override
            public String call() throws Exception {
                return "returning from 'Task 3'";
            }
        });
        Task task4 = new Task(new UUID(1, 4), group2, TaskType.WRITE, new Callable() {
            @Override
            public String call() throws Exception {
                return "returning from 'Task 4'";
            }
        });
        Queue<Task> tasks = new LinkedList<>();
        tasks.add(task1);
        tasks.add(task2);
        tasks.add(task3);
        tasks.add(task4);
        TaskExecutorService taskService = new TaskExecutorService(tasks);
        taskService.submit();
        taskService.shutdown();
    }
}

class TaskExecutorService implements TaskExecutor {
    Queue<Task> tasks;
    ExecutorService service = Executors.newFixedThreadPool(3);

    public TaskExecutorService(Queue<Task> tasks) {
        this.tasks = tasks;
    }

    public List<Future> submit() {
        List<Future> results = new ArrayList<>();
        synchronized (tasks) {
            while (!tasks.isEmpty()) {
                synchronized (tasks) {
                    Task taskToExecute = tasks.poll();
                    results.add(submitTask(taskToExecute));
                }
            }
        }
        return results;
    }

    @Override
    public Future submitTask(Task task) {
        Future<Task> result = service.submit(task.taskAction());
        try {
            System.out.println(result.get());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public void shutdown() {
        if (!this.service.isShutdown()) {
            this.service.shutdown();
        }
    }
}

enum TaskType {
    READ,
    WRITE,
}

interface TaskExecutor {
    Future submitTask(Task task);
}

record Task(
        UUID taskUUID,
        TaskGroup taskGroup,
        TaskType taskType,
        Callable taskAction
) {
    public Task {
        if (taskUUID == null || taskGroup == null || taskType == null ||
                taskAction == null) {
            throw new IllegalArgumentException("All parameters must not be null");
        }
    }
}

record TaskGroup(
        UUID groupUUID
) {
    public TaskGroup {
        if (groupUUID == null) {
            throw new IllegalArgumentException("All parameters must not be null");
        }
    }
}
