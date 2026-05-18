import { Sequelize } from 'sequelize';
import env from './env.config.js';

const sequelize = new Sequelize({
    dialect: 'postgres',
    host: env.DB_HOST,
    port: env.DB_PORT,
    database: env.DB_NAME,
    username: env.DB_USER,
    password: env.DB_PASSWORD,
    schema: env.DB_SCHEMA,
    logging: env.NODE_ENV === 'development' ? console.log : false,
    pool: {
        max: 10,
        min: 2,
        acquire: 30000,
        idle: 10000
    }
});

export async function connectDB() {
    try {
        await sequelize.authenticate();
        await sequelize.query(`SET search_path TO ${env.DB_SCHEMA}`);
        await sequelize.sync({ alter: true });
    } catch(err) {
        console.error('✗ PostgreSQL connection failed:', err.message);
        process.exit(1); //Se usa ahí porque si la base de datos falla al conectarse, la aplicación no podrá funcionar de todos modos y es mejor detenerla por completo.
    }
}

export default sequelize;