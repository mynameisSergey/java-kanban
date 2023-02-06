package manager;

import tasks.Epic;
import tasks.Subtask;
import tasks.Task;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static tasks.Status.NEW;
import static tasks.Status.DONE;
import static tasks.Status.IN_PROGRESS;

import java.time.Instant;
import java.util.*;


import static manager.Managers.getDefaultHistory;


public class InMemoryTaskManager implements TaskManager {
    public int id = 0;
    protected HistoryManager historyManager = (HistoryManager) getDefaultHistory();
    private final Comparator<Task> taskComparator = Comparator.comparing(Task::getStartTime);
    protected Set<Task> prioritizedTasks = new TreeSet<>(taskComparator);
    public Map<Integer, Task> allTasks = new HashMap<>(); // мапа с задачами
    public Map<Integer, Epic> allEpics = new HashMap<>(); // мапа с эпиками
    public Map<Integer, Subtask> allSubtasks = new HashMap<>(); // мапа с подзадачами


    public int generateId() {
        return ++id;
    }

    @Override
    public List<Task> getHistory() { // Получение списка истории просмотренных задач
        return historyManager.getHistory();
    }


    @Override
    public Task getTask(int id) { // получение задачи по айди
        Task task = allTasks.get(id);
        if (task != null) {
            historyManager.add(task);
        }
        return task;
    }

    @Override
    public Epic getEpic(int id) { // получение эпика по айди
        Epic epic = allEpics.get(id);
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic;
    }

    @Override
    public Subtask getSubtask(int id) {// получение подзадачи по айди
        Subtask subtask = allSubtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
        }
        return subtask;
    }

    @Override
    public List<Task> getAllTask() { // получение списка всех задач
        if (allTasks.size() == 0) {
            System.out.println("Список задач пустой");
            return Collections.emptyList();
        }
        return new ArrayList<>(allTasks.values());
    }

    @Override
    public List<Epic> getAllEpic() { // получение списка всех эпиков
        if (allEpics.size() == 0) {
            System.out.println("Список эпиков пустой");
            return Collections.emptyList();
        }
        return new ArrayList<>(allEpics.values());
    }

    @Override
    public List<Subtask> getAllSubtask() { // получение списка всех подзадач
        if (allSubtasks.size() == 0) {
            System.out.println("Список подзадач пустой");
            return Collections.emptyList();
        }
        return new ArrayList<>(allSubtasks.values());
    }

    @Override
    public List<Subtask> getIdSubtask(int id) { // Получение списка всех подзадач определённого эпика.
        if (allEpics.containsKey(id)) {
            List<Subtask> subtasksNew = new ArrayList<>();
            Epic epic = allEpics.get(id);
            for (int i = 0; i < epic.getSubTaskId().size(); i++) {
                subtasksNew.add(allSubtasks.get(epic.getSubTaskId().get(i)));
            }
            return subtasksNew;
        } else {
            return Collections.emptyList();

        }
    }


    @Override
    public Task addNewTask(Task task) { // добавление задачи в мапу
        if (task == null) return null;
        int newTaskId = generateId();
        task.setId(newTaskId);
        addNewPrioritizedTask(task);
        allTasks.put(newTaskId, task);
        return task;
    }

    @Override
    public Epic addNewEpic(Epic epic) { // дробавляем новый эпик
        if (epic == null) return null;
        int newEpicId = generateId();
        epic.setId(newEpicId);
        allEpics.put(newEpicId, epic);
        return epic;
    }

    @Override
    public Subtask addNewSubtask(Subtask subtask) { // добавляем новые подзадачи
        if (subtask == null) return null;
        int newSubtaskId = generateId();
        subtask.setId(newSubtaskId);
        Epic epic = allEpics.get(subtask.getEpicId());
        if (epic != null) {
            addNewPrioritizedTask(subtask);
            allSubtasks.put(newSubtaskId, subtask);
            epic.setSubtaskIds(newSubtaskId);
            updateStatusEpic(epic);
            updateTimeEpic(epic);
            return subtask;
        } else {
            System.out.println("Эпик не найден");
            return subtask;
        }
    }


    @Override
    public void removeAllTask() { // удаление всех задач из мапы
        if (!allTasks.isEmpty()) {
            for (Task task : allTasks.values()) {
                historyManager.remove(task.getId());
            }
            allTasks.clear();
            prioritizedTasks.clear();
        }
    }

    @Override
    public void removeAllEpic() { // удаление всех эпиков из мапы
        if (!allEpics.isEmpty()) {
            for (Subtask subtask : allSubtasks.values()) {
                historyManager.remove(subtask.getId());
            }
            allSubtasks.clear();
            for (Epic epic : allEpics.values()) {
                historyManager.remove(epic.getId());
            }
            allEpics.clear();
        }
    }

    @Override
    public void removeAllSubtask() { // удаление всех подзадач из мапы
        for (Epic epic : allEpics.values()) {
            for (int subtaskId : epic.getSubTaskId()) {
                Subtask subtask = allSubtasks.get(subtaskId);
                prioritizedTasks.remove(subtask);
                allSubtasks.remove(subtaskId);
                historyManager.remove(subtaskId);
            }
            epic.getSubTaskId().clear();
        }
    }

    @Override
    public void removeAllSubtaskByEpic(Epic epic) {
        if (epic != null) {
            for (int subtaskId : epic.getSubTaskId()) {
                Subtask subtask = allSubtasks.get(subtaskId);
                prioritizedTasks.remove(subtask);
                allSubtasks.remove(subtaskId);
                historyManager.remove(subtaskId);
            }
            epic.getSubTaskId().clear();
        }
    }


    @Override
    public void removeTaskId(int id) { // удаление задачи из мапы по айди
        if (allTasks.containsKey(id)) {
            prioritizedTasks.removeIf(allTasks -> allTasks.getId() == id);
            allTasks.remove(id);
            historyManager.remove(id);
            allTasks.remove(id);
        }
    }

    private List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritizedTasks);
    }


    public void updateTimeEpic(Epic epic) { // обновление времени эпика
        List<Subtask> subtasks = getIdSubtask(epic.getId());
        Instant startTime = subtasks.get(0).getStartTime();
        Instant endTime = subtasks.get(0).getEndTime();

        for (Subtask subtask : subtasks) {
            if (subtask.getStartTime().isBefore(startTime)) startTime = subtask.getStartTime();
            if (subtask.getEndTime().isAfter(endTime)) endTime = subtask.getEndTime();
        }

        epic.setStartTime(startTime);
        epic.setEndTime(endTime);
        long duration = (endTime.toEpochMilli() - startTime.toEpochMilli());
        epic.setDuration(duration);
    }

    @Override
    public void updateTask(Task task) {
        if (task != null && allTasks.containsKey(task.getId())) {
            addNewPrioritizedTask(task);
            allTasks.put(task.getId(), task);
        } else {
            System.out.println("Задача не найдена");
        }
    }

    @Override
    public void updateEpic(Epic epic) {
        if (epic != null && allEpics.containsKey(epic.getId())) {
            allEpics.put(epic.getId(), epic);
            updateStatusEpic(epic);
            updateTimeEpic(epic);
        } else {
            System.out.println("Эпик не найден");
        }
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (subtask != null && allSubtasks.containsKey(subtask.getId())) {
            addNewPrioritizedTask(subtask);
            allSubtasks.put(subtask.getId(), subtask);
            Epic epic = allEpics.get(subtask.getEpicId());
            updateStatusEpic(epic);
            updateTimeEpic(epic);
        } else {
            System.out.println("Подзадача не найдена");
        }
    }

    @Override
    public void updateStatusEpic(Epic epic) { // обновление статуса эпика
        if (allEpics.containsKey(epic.getId())) {
            if (epic.getSubTaskId().size() == 0) {
                epic.setStatus(NEW);
            } else {
                List<Subtask> subtasksNew = new ArrayList<>();
                int countDone = 0;
                int countNew = 0;

                for (int i = 0; i < epic.getSubTaskId().size(); i++) {
                    subtasksNew.add(allSubtasks.get(epic.getSubTaskId().get(i)));
                }

                for (Subtask subtask : subtasksNew) {
                    if (subtask.getStatus() == DONE) {
                        countDone++;
                    }
                    if (subtask.getStatus() == NEW) {
                        countNew++;
                    }
                    if (subtask.getStatus() == IN_PROGRESS) {
                        epic.setStatus(IN_PROGRESS);
                        return;
                    }
                }

                if (countDone == epic.getSubTaskId().size()) {
                    epic.setStatus(DONE);
                } else if (countNew == epic.getSubTaskId().size()) {
                    epic.setStatus(NEW);
                } else {
                    epic.setStatus(IN_PROGRESS);
                }
            }
        } else {
            System.out.println("Эпик не найден");
        }
    }

    public void getStatusEpic(int id) { // Управление статусами эпиков
        int statusSubNew = 0;
        int statusSubDone = 0;

        for (Integer idSub : allEpics.get(id).getSubTaskId()) {
            if (allSubtasks.get(idSub).getStatus().equals(NEW)) {
                statusSubNew++;
            } else if (allSubtasks.get(idSub).getStatus().equals(DONE)) {
                statusSubDone++;
            }
            if ((allEpics.get(id).getSubTaskId() == null) || (statusSubNew == allEpics.get(id).getSubTaskId().size())) {
                allEpics.get(id).setStatus(NEW);
            } else if (statusSubDone == allEpics.get(id).getSubTaskId().size()) {
                allEpics.get(id).setStatus(DONE);
            } else {
                allEpics.get(id).setStatus(IN_PROGRESS);
            }
        }
    }

    private void addNewPrioritizedTask(Task task) {
        prioritizedTasks.add(task);
        validateTaskPriority();
    }

    public boolean checkTime(Task task) {
        List<Task> tasks = List.copyOf(prioritizedTasks);
        int sizeTimeNull = 0;
        if (tasks.size() > 0) {
            for (Task taskSave : tasks) {
                if (taskSave.getStartTime() != null && taskSave.getEndTime() != null) {
                    if (task.getStartTime().isBefore(taskSave.getStartTime())
                            && task.getEndTime().isBefore(taskSave.getStartTime())) {
                        return true;
                    } else if (task.getStartTime().isAfter(taskSave.getEndTime())
                            && task.getEndTime().isAfter(taskSave.getEndTime())) {
                        return true;
                    }
                } else {
                    sizeTimeNull++;
                }

            }
            return sizeTimeNull == tasks.size();
        } else {
            return true;
        }
    }

    private void validateTaskPriority() {
        List<Task> tasks = getPrioritizedTasks();

        for (int i = 1; i < tasks.size(); i++) {
            Task task = tasks.get(i);

            boolean taskHasIntersections = checkTime(task);

            if (taskHasIntersections) {
                try {
                    throw new ManagerValidateException(
                            "Задачи " + task.getId() + " и " + tasks.get(i - 1) + "пересекаются");
                } catch (ManagerSaveException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

























