package net.jemsit.common.clients.auth;

import net.jemsit.common.clients.FeignAuthInterceptor;
import net.jemsit.common.dto.request.auth.ProfileRequestDTO;
import net.jemsit.common.dto.response.auth.ProfileResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "profile-service", path = "/api/profile", configuration = FeignAuthInterceptor.class)
public interface ProfileServiceClient {

    @PostMapping("/v1/create")
    ProfileResponseDTO createProfile(@RequestBody ProfileRequestDTO request);

    @GetMapping("/v1/{id}")
    ProfileResponseDTO getProfileById(@PathVariable("id") Long id);

    @PutMapping("v1/update/{id}")
    ProfileResponseDTO updateProfile(@PathVariable("id") Long profileId, @RequestBody ProfileRequestDTO request);

}
