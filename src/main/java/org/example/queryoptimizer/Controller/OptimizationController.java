//package org.example.queryoptimizer.Controller;
//
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.example.queryoptimizer.Dto.Optimization.OptimizationRequestDto;
//import org.example.queryoptimizer.Dto.Optimization.OptimizationResponseDto;
//import org.example.queryoptimizer.Dto.response.ApiResponse;
//import org.example.queryoptimizer.Service.OptimizationService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/v1/optimization")
//@RequiredArgsConstructor
//public class OptimizationController {
//
//    private final OptimizationService optimizationService;
//
//    @PostMapping("/analyze")
//    public ResponseEntity<ApiResponse<OptimizationResponseDto>> optimize(
//            @Valid @RequestBody OptimizationRequestDto request) {
//
//        OptimizationResponseDto result =
//                optimizationService.optimize(request);
//
//        ApiResponse<OptimizationResponseDto> response =
//                new ApiResponse<>(
//                        true,
//                        "Optimization analysis completed",
//                        result
//                );
//
//        return ResponseEntity.ok(response);
//    }
//}
