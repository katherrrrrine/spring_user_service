package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.UserDto;
import org.example.exception.*;
import org.example.mapper.UserMapper;
import org.example.model.User;
import org.example.dao.UserRepository;
import org.example.validation.UserInputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic.user-events:user-events}")
    private String userEventsTopic;

    @Autowired
    public UserService(UserRepository userRepository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public UserDto createUser(UserDto userDto) {
        UserInputValidator.validateUserDto(userDto);
        userDto.setEmail(userDto.getEmail().toLowerCase());
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new UserValidationException("Пользователь с email " + userDto.getEmail() + " уже существует");
        }

        User user = UserMapper.toEntity(userDto);
        User savedUser = userRepository.save(user);
        sendUserEvent(savedUser.getId(), savedUser.getEmail(), "CREATED");
        return UserMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findUserById(Long id) {
        if (id == null || id <= 0) {
            throw new UserValidationException("Неверный ID пользователя");
        }

        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.map(UserMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAllUsers() {
        try {
            List<UserDto> users = userRepository.findAll().stream()
                    .map(UserMapper::toDto)
                    .collect(Collectors.toList());
            return users;
        } catch (Exception e) {
            throw new UserDaoException("Ошибка при получении списка пользователей", e);
        }
    }

    public UserDto updateUser(UserDto userDto) {
        if (userDto == null || userDto.getId() == null) {
            throw new UserValidationException("ID пользователя обязателен для обновления");
        }

        UserInputValidator.validateUserDto(userDto);
        userDto.setEmail(userDto.getEmail().toLowerCase());

        Optional<User> existingUserOpt = userRepository.findById(userDto.getId());
        if (existingUserOpt.isEmpty()) {
            throw new UserNotFoundException("Пользователь с ID " + userDto.getId() + " не найден");
        }

        User existingUser = existingUserOpt.get();

        if (!existingUser.getEmail().equals(userDto.getEmail()) &&
                userRepository.existsByEmail(userDto.getEmail())) {
            throw new UserValidationException("Пользователь с email " + userDto.getEmail() + " уже существует");
        }

        UserMapper.updateEntityFromDto(userDto, existingUser);

        try {
            User updatedUser = userRepository.save(existingUser);
            return UserMapper.toDto(updatedUser);
        } catch (Exception e) {
            throw new UserDaoException("Ошибка при обновлении пользователя с ID: " + userDto.getId(), e);
        }
    }

    public void deleteUser(Long id) {
        if (id == null || id <= 0) {
            throw new UserValidationException("Неверный ID пользователя для удаления");
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("Пользователь с ID " + id + " не найден");
        }

        User user = userOpt.get();
        String email = user.getEmail();
        try {
            userRepository.deleteById(id);
            sendUserEvent(id, email, "DELETED");
        } catch (Exception e) {
            throw new UserDaoException("Ошибка при удалении пользователя с ID: " + id, e);
        }
    }

    private void sendUserEvent(Long userId, String email, String eventType) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("email", email);
            event.put("userId", userId);
            event.put("timestamp", LocalDateTime.now().toString());

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(userEventsTopic, eventJson);
        } catch (Exception e) {
            throw new SendMessageException("Ошибка при отправке сообщения");
        }
    }
}