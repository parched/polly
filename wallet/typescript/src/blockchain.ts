import { Base64 } from 'js-base64';

interface Block {
    prevHash: Uint8Array,
    data: Uint8Array,
    modifer: number,
}

export function decode(blockchain: any): Block[] {
    // if (typeof block.prev_hash !== "string"
    // || typeof block.data !== "string" 
    // || typeof block.modfier !== "number") {
    //     return undefined;
    // }

    if (!Array.isArray(blockchain)) {
        throw new TypeError(`expected an array but got $blockchain`)
    }

    return blockchain.map(block => {
        const decoded: Block = {
            prevHash: Base64.toUint8Array(block.prev_hash),
            data: Base64.toUint8Array(block.data),
            modifer: block.modifer
        };
        return decoded;
    });
}
