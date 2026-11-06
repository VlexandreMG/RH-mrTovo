package com.mrtovo.backend.entity;

/** Ce qu'une case du calendrier represente. Le manager ne recoit que ABSENT et EN_ATTENTE. */
public enum NatureCaseCalendrier {

    CONGE,
    EN_ATTENTE,
    ABSENCE_A_QUALIFIER,
    ABSENCE_JUSTIFIEE,
    ABSENCE_NON_JUSTIFIEE,
    ABSENT
}
