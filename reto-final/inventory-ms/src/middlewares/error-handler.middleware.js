import env from "../config/env.config.js";

/* 
    En Express, si una función tiene exactamente cuatro argumentos (err, req, res, next),
    Express la registra automáticamente como un Manejador de Errores. 
    Cualquier error que ocurra en tus rutas y sea "arrojado" (throw) 
    o pasado a través de next(error) terminará cayendo aquí directamente.
*/

export function errorHandler(err, req, res, next) {
    const statusCode = err.statusCode || 500;
    const message = statusCode === 500 && env.NODE_ENV === 'production'
        ? 'Internal server error'
        : err.message;
    
    if (statusCode === 500) console.error('ERROR:', err);

    res.status(statusCode).json({
        success: false,
        message,
        ... (env.NODE_ENV === 'development' && {stack: err.stack }), //El operador ... En JavaScript, si haces true && { objeto }, el resultado es { objeto } este se integra al padre
    })
}
