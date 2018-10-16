package agents.piers.evolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import agents.piers.FallbackRule;
import agents.piers.IRule;
import agents.piers.RandomUtils;
import agents.piers.RuleSequenceRule;
import hanabAI.Action;
import hanabAI.Agent;
import hanabAI.State;

public class Genome {

    private ArrayList<GenomeRule> dna;
    private String name;

    private Genome(ArrayList<GenomeRule> dna) {
        this.dna = dna;
        this.name = UUID.randomUUID().toString();
    }

    public static Genome crossover(Genome X, float xFitness, Genome Y, float yFitness) {
        ArrayList<GenomeRule> childDna = new ArrayList<GenomeRule>();
        Genome strongerGenome = xFitness >= yFitness ? X : Y;
        Genome weakerGenome = xFitness >= yFitness ? Y : X;

        /* For the length of genes they have in common take the one
         * from the stronger genome with higher liklihood.
         */
        int sharedGenomeLength = Math.min(X.dna.size(), Y.dna.size());
        for (int i = 0; i < sharedGenomeLength; i++) {
            childDna.add(
                GenomeRule.crossover(
                    X.dna.get(i),
                    xFitness,
                    Y.dna.get(i),
                    yFitness
                )
            );
        }

        /* If they were of equal length we will have already considerd
         * the genetic material of both parents.
         */
        if (X.dna.size() == Y.dna.size()) {
            return new Genome(childDna);
        }

        Genome longerGenome = X.dna.size() > Y.dna.size() ? X : Y;
        int longerGenomeLength = longerGenome.dna.size();
        for (int i = sharedGenomeLength; i < longerGenomeLength; i++) {
            if (strongerGenome == longerGenome
                    && RandomUtils.chance(0.9)
            ) {
                childDna.add(longerGenome.dna.get(i));
            } else if (weakerGenome == longerGenome
                    && RandomUtils.chance(0.4)
            ) {
                childDna.add(longerGenome.dna.get(i));
            }
        }

        return new Genome(childDna);
    }

    public static Genome mutate(Genome X) {
        @SuppressWarnings("unchecked")
        ArrayList<GenomeRule> mutatedDna = (ArrayList<GenomeRule>)X.dna.clone();

        /* Every 10000 lives drop a random segment of dna */
        if (RandomUtils.chance(1.0 / 10000.0) && mutatedDna.size() > 1) {
            mutatedDna.remove(RandomUtils.choose(mutatedDna));
        }
        /* Every 10000 lives add a random segment of dna */
        if (RandomUtils.chance(1.0 / 10000.0) && mutatedDna.size() <= 20) {
            mutatedDna.add(GenomeRule.spawnRandom());
        }
        /* Every 5000 lives swap a segment of dna around */
        if (RandomUtils.chance(1.0 / 5000.0) && mutatedDna.size() > 3) {
            GenomeRule a = RandomUtils.choose(mutatedDna);
            GenomeRule b = RandomUtils.choose(mutatedDna);
            int aIndex = mutatedDna.indexOf(a);
            int bIndex = mutatedDna.indexOf(b);
            mutatedDna.set(aIndex, b);
            mutatedDna.set(bIndex, a);
        }

        for (int i = 0; i < mutatedDna.size(); i++) {
            mutatedDna.set(i, GenomeRule.mutate(mutatedDna.get(i)));
        }

        return new Genome(mutatedDna);
    }

    public String getName() { return this.name; }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.name);
        builder.append("(");
        for (int i = 0; i < this.dna.size(); i++) {
            GenomeRule gene = this.dna.get(i);
            builder.append(gene.toString());
            if (i < this.dna.size() - 1) {
                builder.append("...");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    public String formatShortDna() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int i = 0; i < this.dna.size(); i++) {
            GenomeRule gene = this.dna.get(i);
            builder.append(gene.toShortString());
            if (i < this.dna.size() - 1) {
                builder.append("-");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    public static Agent asAgent(Genome X, int playerIndex) {
        ArrayList<IRule> rules = new ArrayList<IRule>();
        for (GenomeRule gene : X.dna) {
            rules.add(GenomeRule.asRule(gene, playerIndex));
        }
        rules.add(new FallbackRule(playerIndex));
        IRule policy = new RuleSequenceRule(rules);
        return new Agent() {
            @Override
            public String toString() {
                return X.toString();
            }

            @Override
            public Action doAction(State s) {
				return policy.play(s);
			}
        };
    }

    public static Genome spawnRandom() {
        ArrayList<GenomeRule> dna = new ArrayList<GenomeRule>();
        int length = 5 + RandomUtils.integer(1, 4);
        for (int i = 1; i <= length; i++) {
            dna.add(GenomeRule.spawnRandom());
        }
        return new Genome(dna);
    }

    public static Genome spawnModel() {
        ArrayList<GenomeRule> dna = new ArrayList<GenomeRule>();
        ArrayList<Float> playSemiSafeWeights = new ArrayList<Float>(
            Arrays.asList(
                new Float[]
                {
                    (float)0.7,
                    (float)0.1,
                    (float)0.1,
                    (float)0.1,
                    (float)-0.3,
                    (float)0.1,
                    (float)0.5,
                    (float)0.8
                }
            )
        );
        ArrayList<Float> weights = new ArrayList<Float>(
            Arrays.asList(
                new Float[]
                {
                    (float)0.0,
                    (float)0.1,
                    (float)0.1,
                    (float)0.1,
                    (float)-0.3,
                    (float)0.1,
                    (float)0.5,
                    (float)0.8
                }
            )
        );
        dna.add(
            new GenomeRule(
                GenomeRuleType.PlaySafe,
                2,
                0,
                weights
            )
        );
        dna.add(
            new GenomeRule(
                GenomeRuleType.PlayProbablySafe,
                2,
                0,
                playSemiSafeWeights
            )
        );
        dna.add(
            new GenomeRule(
                GenomeRuleType.TellAnyonePlayable,
                0,
                0,
                weights
            )
        );
        dna.add(
            new GenomeRule(
                GenomeRuleType.OsawaDiscard,
                0,
                0,
                weights
            )
        );
        dna.add(
            new GenomeRule(
                GenomeRuleType.TellAnyoneUseless,
                0,
                0,
                weights
            )
        );
        dna.add(
            new GenomeRule(
                GenomeRuleType.TellAnyoneUseful,
                0,
                0,
                weights
            )
        );
        dna.add(
            new GenomeRule(
                GenomeRuleType.RandomDiscard,
                0,
                0,
                weights
            )
        );

        Genome modelGenome = new Genome(dna);
        for (int i = 1; i <= 30; i++) {
            modelGenome = Genome.mutate(modelGenome);
        }
        return modelGenome;
    }
}