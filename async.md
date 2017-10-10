# Asynchronous and distributed linguistic annotation
## Status Quo
 * Some annotators are multi-threaded internally and/or not thread-safe
 * Some annotators could be run in multiple threads
  - as multiple copies: AsyncParserAnnotator
  - asynchronous in a single copy: JerseyAnnotator
  - in one thread, but with other annotators
## Massively parallel annotation
 * Basis: JerseyAnnotator + exml-server
 * Alternative to GNU parallel or Spark
 * Use multiple copies of a service to distribute execution(s)
 * Use a registry for annotation services
    - annotator services register for the registry with the pipe(s) that
      they support
      - realize annotator services as (docker-cloud) containers that carry code+models
      - containers get the name of the registry via standard DNS name or via Env variable
    - a RegistryAnnotator service fetches endpoints and talks directly
      to the annotator services
 * Nice to haves:
    - Backpressure to avoid creep-up of jobs stuck at one particular stage
      - currently every stuck job will consume a thread
    - Need-based resource allocation: spin up new copies of annotators
    - document disaggregation+joining

## Publicity stunts
 * Run NN-based models on instances with GPUs
 * Build a nice frontend
   - JS representation of EXML data
   - programmable visualization