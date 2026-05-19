export function validateRequest(schema) {
    return (req, res, next) => {

        const {error, value} = schema.validate(req.body, {
            abortEarly: false,
            stripUnknown: true
        });

        if(error) {
            const details = error.details.map(d=> ({
                field: d.path.join('.'),
                message: d.message
            }));

            return res.status(400).json({success: false, error: details})
        }

        req.body = value;
        next();
    }
}