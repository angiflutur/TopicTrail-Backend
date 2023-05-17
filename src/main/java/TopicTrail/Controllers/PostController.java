package TopicTrail.Controllers;

import TopicTrail.Domain.Group;
import TopicTrail.Domain.Post;
import TopicTrail.Domain.User;
import TopicTrail.Security.JWTUtil;
import TopicTrail.Services.GroupService;
import TopicTrail.Services.PostService;
import TopicTrail.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Controller
@ResponseBody
public class PostController {
    private final PostService postService;
    private final JWTUtil jwtUtil;
    private final UserService userService;
    private final GroupService groupService;

    public PostController(PostService postService, JWTUtil jwtUtil, UserService userService, GroupService groupService) {
        this.postService = postService;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.groupService = groupService;
    }

    @PostMapping("/post/new")
    public Mono<Post> newPost(@Valid Post post, @RequestHeader(name = "Authorization") String authorizationHeader){
        if(post.getId().length() != 36){
            post.setId(UUID.randomUUID().toString());

            authorizationHeader = authorizationHeader.substring(7);
            String username = jwtUtil.getUsernameFromToken(authorizationHeader);
            post.setUsername(username);

            Mono<User> user = userService.findByUsername(username);
            Mono<Group> group = groupService.findByTitle(post.getGroup());

            user.flatMap(u -> {
                u.getPosts().add(post.getId());
                return userService.save(u);
            });

            group.flatMap(g -> {
                g.getPosts().add(post.getId());
                return groupService.save(g);
            });

            Mono<Post> savedPost = postService.save(post);

            return savedPost;
        }else
            return postService.update(post);
    }

    @GetMapping("/post/all")
    Flux<Post> getPosts(@RequestParam(required = false) String groupName, @RequestParam(required = false) String username,
                        @RequestParam(required = false) String favorite, @RequestHeader(name = "Authorization") String authorizationHeader){
        String token = authorizationHeader.substring(7);
        Flux<Post> posts = postService.getPosts();
        if(groupName != null)
            posts = posts.filter(x -> x.getGroup().equals(groupName));
        if(username != null)
            posts = posts.filter(x -> x.getUsername().equals(username));
        return posts;
    }
}
