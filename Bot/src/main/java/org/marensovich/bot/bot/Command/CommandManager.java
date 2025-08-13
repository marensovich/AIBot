package org.marensovich.bot.bot.Command;

import lombok.extern.slf4j.Slf4j;
import org.marensovich.bot.bot.Bot;
import org.marensovich.bot.bot.Command.Interfaces.AdminCommand;
import org.marensovich.bot.bot.Command.Interfaces.Command;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CommandManager {
    private final Map<String, Command> commands = new HashMap<>();
    private final Map<String, AdminCommand> adminCommands = new HashMap<>();
    private final Map<Long, Command> activeCommands = new HashMap<>();

    @Autowired
    public CommandManager(List<Command> commandList, List<AdminCommand> adminCommandList) {
        commandList.forEach(this::registerCommand);
        adminCommandList.forEach(this::registerAdminCommand);
        //this.userService = userService;
    }

    private void registerCommand(Command command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    private void registerAdminCommand(AdminCommand command) {
        adminCommands.put(command.getName().toLowerCase(), command);
    }

    public boolean executeCommand(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            log.debug("Update doesn't contain message or text");
            return false;
        }
        String messageText = update.getMessage().getText().trim();
        String commandText = extractCommand(messageText).toLowerCase();
        Command command = findCommand(commandText);
        if (command == null) {
            log.warn("Command not found for: '{}'", commandText);
            return false;
        }
        if (command instanceof AdminCommand) {
            AdminCommand adminCommand = (AdminCommand) command;
            if (adminCommand.isAdminRequired() && !isAdmin(update)) {
                Bot.getInstance().sendNoAccessMessage(update);
                return true;
            }
        }
        command.execute(update);
        return true;
    }

    private String extractCommand(String text) {
        if (text.contains("@")) {
            text = text.substring(0, text.indexOf("@"));
        }
        return text.trim().toLowerCase();
    }

    private Command findCommand(String input) {
        Command command = commands.get(input);
        if (command != null) {
            return command;
        }
        return adminCommands.get(input);
    }

    private boolean isAdmin(Update update) {
        return false;
        //return userService.IsAdmin(update.getMessage().getFrom().getId());
    }


    public void setActiveCommand(Long userId, Command command) {
        log.debug("Активная команда " + command.getName() + " закреплена за пользователем " + userId);
        activeCommands.put(userId, command);
    }

    public void unsetActiveCommand(Long userId) {
        log.debug("Активная команда пользователя " + userId + " была очищена");
        activeCommands.remove(userId);
    }

    public boolean hasActiveCommand(Long userId) {
        return activeCommands.containsKey(userId);
    }

    public Command getActiveCommand(Long userId) {
        return activeCommands.get(userId);
    }

}