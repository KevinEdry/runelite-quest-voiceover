import * as restate from "@restatedev/restate-sdk";
import "dotenv/config";

import { questVoiceoverWorkflow } from "./workflows/quest-voiceover.workflow.js";
import { cleanupVoicesWorkflow } from "./workflows/cleanup-voices.workflow.js";
import { backfillFemaleVoicesWorkflow } from "./workflows/backfill-female-voices.workflow.js";

const port = parseInt(process.env.RESTATE_SERVICE_PORT || "9080");

restate.serve({
  services: [questVoiceoverWorkflow, cleanupVoicesWorkflow, backfillFemaleVoicesWorkflow],
  port,
});

console.log(`Restate service listening on port ${port}`);
