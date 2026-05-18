import app from "./src/app.js";
import env from "./src/config/env.config.js";
import { connectDB } from "./src/config/database.config.js";

async function bootstrap() {
    await connectDB();

    app.listen(env.PORT, ()=> {
        console.log(`✓ inventory-ms running on http://localhost:${env.PORT}`);
        console.log(`  Health:  http://localhost:${env.PORT}/health`);
        console.log(`  Metrics: http://localhost:${env.PORT}/metrics`);
    })
}

bootstrap().catch(err => {
  console.error('✗ Failed to start:', err);
  process.exit(1);
});