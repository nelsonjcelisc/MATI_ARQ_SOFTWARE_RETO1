import Joi from "joi";

const uuid = Joi.string().uuid({ version: 'uuidv4' }).required();

export const createItemSchema = Joi.object({
    stickerId: uuid,
    ownerId: uuid
});

export const transferOwnershipSchema = Joi.object({
    fromId: uuid,
    toId: uuid
});