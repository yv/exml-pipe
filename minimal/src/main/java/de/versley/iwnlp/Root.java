
package de.versley.iwnlp;

import java.util.ArrayList;
import java.util.List;


public class Root
{
    private List<WordForm> wordFormList = new ArrayList<WordForm>();

    /** 
     * Get the list of 'WordForm' element items.
     * 
     * @return list
     */
    public List<WordForm> getWordFormList() {
        return wordFormList;
    }

    /** 
     * Set the list of 'WordForm' element items.
     * 
     * @param list
     */
    public void setWordFormList(List<WordForm> list) {
        wordFormList = list;
    }

    public static class WordForm
    {
        private String form;
        private List<LemmatizerItem> lemmaList = new ArrayList<LemmatizerItem>();

        /** 
         * Get the 'Form' element value.
         * 
         * @return value
         */
        public String getForm() {
            return form;
        }

        /** 
         * Set the 'Form' element value.
         * 
         * @param form
         */
        public void setForm(String form) {
            this.form = form;
        }

        /** 
         * Get the list of 'LemmatizerItem' element items.
         * 
         * @return list
         */
        public List<LemmatizerItem> getLemmaList() {
            return lemmaList;
        }

        /** 
         * Set the list of 'LemmatizerItem' element items.
         * 
         * @param list
         */
        public void setLemmaList(List<LemmatizerItem> list) {
            lemmaList = list;
        }

        public static class LemmatizerItem
        {
            private String POS;
            private String form;
            private String lemma;

            /** 
             * Get the 'POS' element value.
             * 
             * @return value
             */
            public String getPOS() {
                return POS;
            }

            /** 
             * Set the 'POS' element value.
             * 
             * @param POS
             */
            public void setPOS(String POS) {
                this.POS = POS;
            }

            /** 
             * Get the 'Form' element value.
             * 
             * @return value
             */
            public String getForm() {
                return form;
            }

            /** 
             * Set the 'Form' element value.
             * 
             * @param form
             */
            public void setForm(String form) {
                this.form = form;
            }

            /** 
             * Get the 'Lemma' element value.
             * 
             * @return value
             */
            public String getLemma() {
                return lemma;
            }

            /** 
             * Set the 'Lemma' element value.
             * 
             * @param lemma
             */
            public void setLemma(String lemma) {
                this.lemma = lemma;
            }
        }
    }
}
