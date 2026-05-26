MIRRORING

HTTP                                    STOMP
@GetMapping / @PostMapping              @MessageMapping
@PathVariable                           @DestinationVariable
@RequestBody                            @Payload
@RequestParam                           @Header
ResponseEntity<T>                       @SendTo / @SendToUser
@ExceptionHandler                       @MessageExceptionHandler