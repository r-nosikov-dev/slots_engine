package com.slotengine.model;

/**
 * How line/ways wins are counted on the visible window.
 * Cascade behaviour is an extra feature layered on top of PAYLINES or WAYS.
 */
public enum EvaluationMode {
    /** Classic left-to-right paylines (optionally both-ways). */
    PAYLINES,
    /** All-ways: product of matching symbols on consecutive reels (243/1024/...). */
    WAYS
}
