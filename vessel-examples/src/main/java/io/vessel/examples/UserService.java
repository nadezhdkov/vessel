package io.vessel.examples;

import io.vessel.core.annotation.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserService {

    private final List<User> users = new ArrayList<>(List.of(
            new User(1, "Alice", "alice@example.com"),
            new User(2, "Bob", "bob@example.com")));

    public List<User> findAll() {
        return List.copyOf(users);
    }

    public User findById(long id) {
        return users.stream()
                .filter(user -> user.id() == id)
                .findFirst()
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User save(User user) {
        users.add(user);
        return user;
    }
}
